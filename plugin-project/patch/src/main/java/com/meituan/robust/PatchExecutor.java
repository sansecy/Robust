package com.meituan.robust;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * Created by c_kunwu on 16/5/20.
 * if you need,you can override PatchExecutor
 */
public class PatchExecutor extends Thread {
    protected Context context;
    protected PatchManipulate patchManipulate;
    protected RobustCallBack robustCallBack;

    public PatchExecutor(Context context, PatchManipulate patchManipulate, RobustCallBack robustCallBack) {
        this.context = context;
        this.patchManipulate = patchManipulate;
        this.robustCallBack = robustCallBack;
    }

    @Override
    public void run() {
        try {
            //拉取补丁列表
            List<Patch> patches = fetchPatchList();
            //应用补丁列表
            applyPatchList(patches);
        } catch (Throwable t) {
            RobustLog.e("robust", "PatchExecutor run", t);
            robustCallBack.exceptionNotify(t, "class:PatchExecutor,method:run,line:36");
        }
    }

    /**
     * 拉取补丁列表
     */
    protected List<Patch> fetchPatchList() {
        return patchManipulate.fetchPatchList(context);
    }

    /**
     * 应用补丁列表
     */
    protected void applyPatchList(List<Patch> patches) {
        if (null == patches || patches.isEmpty()) {
            RobustLog.d("robust", " patchManipulate list size is empty");
            return;
        }
        RobustLog.d("robust", " patchManipulate list size is " + patches.size());
        for (Patch p : patches) {
            if (p.isAppliedSuccess()) {
                RobustLog.d("robust", "p.isAppliedSuccess() skip " + p.getLocalPath());
                continue;
            }
            if (patchManipulate.ensurePatchExist(p)) {
                boolean currentPatchResult = false;
                try {
                    currentPatchResult = patch(context, p);
                } catch (Throwable t) {
                    robustCallBack.exceptionNotify(t, "class:PatchExecutor method:applyPatchList patch error:" + t.getMessage());
                }
                if (currentPatchResult) {
                    //设置patch 状态为成功
                    p.setAppliedSuccess(true);
                    //统计PATCH成功率 PATCH成功
                    robustCallBack.onPatchApplied(true, p);

                } else {
                    //统计PATCH成功率 PATCH失败
                    robustCallBack.onPatchApplied(false, p);
                }

                RobustLog.d("robust", "patch LocalPath:" + p.getLocalPath() + ",apply result: " + currentPatchResult);

            } else {
                RobustLog.d("robust", p.getLocalPath() + ",ensurePatchExist result " + false);
            }
        }
    }

    protected boolean patch(Context context, Patch patch) {
        if (!patchManipulate.verifyPatch(context, patch)) {
            RobustLog.e("robust", "patch:" + patch.getLocalPath() + ", verifyPatch result: " + false);
            robustCallBack.logNotify("verifyPatch failure, patch info:" + "id = " + patch.getName() + ",md5 = " + patch.getMd5(), "class:PatchExecutor method:patch line:107");
            return false;
        }

        ClassLoader classLoader = null;

        try {
            File dexOutputDir = getPatchCacheDirPath(context, patch.getName() + patch.getMd5());
            classLoader = new DexClassLoader(patch.getTempPath(), dexOutputDir.getAbsolutePath(),
                    null, PatchExecutor.class.getClassLoader());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        if (null == classLoader) {
            RobustLog.e("robust", "patch:" + patch.getLocalPath() + ", classloader is null");
            return false;
        }

        Class patchClass, sourceClass;

        Class patchesInfoClass;
        PatchesInfo patchesInfo = null;
        try {
            RobustLog.d("robust", "patch patch_info_name:" + patch.getPatchesInfoImplClassFullName());
            patchesInfoClass = classLoader.loadClass(patch.getPatchesInfoImplClassFullName());
            patchesInfo = (PatchesInfo) patchesInfoClass.newInstance();
        } catch (Throwable t) {
            RobustLog.e("robust", "patch failed ,patchesInfoClass.newInstance ", t);
        }

        if (patchesInfo == null) {
            RobustLog.e("robust", "patch patchesInfo is null");
            robustCallBack.logNotify("patchesInfo is null, patch info:" + "id = " + patch.getName() + ",md5 = " + patch.getMd5(), "class:PatchExecutor method:patch line:114");
            return false;
        }

        //classes need to patch
        List<PatchedClassInfo> patchedClasses = patchesInfo.getPatchedClassesInfo();
        if (null == patchedClasses || patchedClasses.isEmpty()) {
//            robustCallBack.logNotify("patchedClasses is null or empty, patch info:" + "id = " + patch.getName() + ",md5 = " + patch.getMd5(), "class:PatchExecutor method:patch line:122");
            //手写的补丁有时候会返回一个空list
            return true;
        }

        boolean isClassNotFoundException = false;
        for (PatchedClassInfo patchedClassInfo : patchedClasses) {
            String patchedClassName = patchedClassInfo.patchedClassName;
            String patchClassName = patchedClassInfo.patchClassName;
            if (TextUtils.isEmpty(patchedClassName) || TextUtils.isEmpty(patchClassName)) {
                robustCallBack.logNotify("patchedClasses or patchClassName is empty, patch info:" + "id = " + patch.getName() + ",md5 = " + patch.getMd5(), "class:PatchExecutor method:patch line:131");
                continue;
            }
            RobustLog.d("robust", "current path:" + patchedClassName);
            try {
                try {
                    sourceClass = classLoader.loadClass(patchedClassName.trim());
                } catch (ClassNotFoundException e) {
                    isClassNotFoundException = true;
//                    robustCallBack.exceptionNotify(e, "class:PatchExecutor method:patch line:258");
                    continue;
                }

                Field[] fields = sourceClass.getDeclaredFields();
                RobustLog.d("robust", "oldClass :" + sourceClass + "     fields " + fields.length);
                Field changeQuickRedirectField = null;
                for (Field field : fields) {
                    if (TextUtils.equals(field.getType().getCanonicalName(), ChangeQuickRedirect.class.getCanonicalName()) && TextUtils.equals(field.getDeclaringClass().getCanonicalName(), sourceClass.getCanonicalName())) {
                        changeQuickRedirectField = field;
                        break;
                    }
                }
                if (changeQuickRedirectField == null) {
                    robustCallBack.logNotify("changeQuickRedirectField  is null, patch info:" + "id = " + patch.getName() + ",md5 = " + patch.getMd5(), "class:PatchExecutor method:patch line:147");
                    RobustLog.d("robust", "current path:" + patchedClassName + " something wrong !! can  not find:ChangeQuickRedirect in" + patchClassName);
                    continue;
                }
                RobustLog.d("robust", "current path:" + patchedClassName + " find:ChangeQuickRedirect " + patchClassName);
                try {
                    patchClass = classLoader.loadClass(patchClassName);
                    Object patchObject = patchClass.newInstance();
                    changeQuickRedirectField.setAccessible(true);
                    changeQuickRedirectField.set(null, patchObject);
                    RobustLog.d("robust", "changeQuickRedirectField set success " + patchClassName);
                } catch (Throwable t) {
                    RobustLog.e("robust", "patch failed! ");
                    robustCallBack.exceptionNotify(t, "class:PatchExecutor method:patch line:163");
                }
            } catch (Throwable t) {
                RobustLog.e("robust", "patch failed! ");
//                robustCallBack.exceptionNotify(t, "class:PatchExecutor method:patch line:169");
            }
        }
        RobustLog.d("robust", "patch finished ");
        if (isClassNotFoundException) {
            return false;
        }
        return true;
    }

    private static final String ROBUST_PATCH_CACHE_DIR = "patch_cache";

    /*
     * @param c
     * @return 返回缓存补丁路径，一般是内部存储,补丁目录
     */
    private static File getPatchCacheDirPath(Context c, String key) {
        File patchTempDir = c.getDir(ROBUST_PATCH_CACHE_DIR + key, Context.MODE_PRIVATE);
        if (!patchTempDir.exists()) {
            patchTempDir.mkdir();
        }

        return patchTempDir;
    }

}
