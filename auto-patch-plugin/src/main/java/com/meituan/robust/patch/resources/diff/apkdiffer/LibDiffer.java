package com.meituan.robust.patch.resources.diff.apkdiffer;

import com.meituan.robust.resource.APKStructure;
import com.meituan.robust.patch.resources.config.RobustResourceConfig;
import com.meituan.robust.resource.diff.data.LibDiffData;

import java.nio.file.Path;

/**
 * Created by hedingxu on 17/5/31.
 */
public class LibDiffer extends BaseDiffer {

    public LibDiffer(RobustResourceConfig config) {
        super(config);
        diffData = new LibDiffData();
        includePatterns = config.libIncludePatterns;
        excludePatterns = config.libExcludePatterns;
        apkResourceType = APKStructure.Lib_Type;
    }

    @Override
    public boolean diffNewFile(Path newFilePath) {
        return super.diffNewFile(newFilePath);
    }

    @Override
    public boolean diffOldFile(Path oldFilePath) {
        return super.diffOldFile(oldFilePath);
    }

    @Override
    protected boolean isNeed(Path filePath) {
        return super.isNeed(filePath);
    }
}