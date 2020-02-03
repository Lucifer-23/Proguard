package com.qihoo.sub;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.kun.brother.proguard.utils.DecryptUtils;
import com.kun.brother.proguard.utils.Zip;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


// 解密与加载多个dex
// 替换真实的Application
public class StubApp extends Application {
    private String mAppName;
    private String mAppVersion;

    private boolean mIsBindReal;
    private Application mDelegate;

    /**
     * ActivityThread创建Application之后调用的第一个函数
     *
     * @param base
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        getMetaData();
        // 获得当前的apk文件
        File apkFile = new File(getApplicationInfo().sourceDir);
        // 把apk文件zip解压到appDir这个目录/data/data/packagename/
        File versionDir = getDir(mAppName + "_" + mAppVersion, MODE_PRIVATE);
        File appDir = new File(versionDir, "app");
        // 提取apk中需要解密的所有dex放入到这个目录
        File dexDir = new File(appDir, "dexDir");
        // 需要我们加载的dex
        List<File> dexFiles = new ArrayList<>();
        // 需要解密(最好再进行MD5文件校验)
        if (dexDir.exists() && dexDir.list().length > 0) {
            // 已经解密过了
            for (File file : dexDir.listFiles()) {
                dexFiles.add(file);
            }
        } else {
            // 把apk解压到appDir
            Zip.unZip(apkFile, appDir);
            // 获取目录下的所有文件
            File[] files = appDir.listFiles();
            for (File file : files) {
                String name = file.getName();
                // 文件名是.dex结尾， 并且不是主dex放入dexDir目录
                if (name.endsWith(".dex") && name.startsWith("kun_brother")) {
                    try {
                        // 从文件中读取byte数组加密后的dex数据
                        byte[] bytes = DecryptUtils.getBytes(file);
                        // 将dex文件解密并且写入原文件file目录
                        DecryptUtils.decrypt(bytes, file.getAbsolutePath());
                        dexFiles.add(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            loadDex(dexFiles, versionDir);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            bindRealApplication();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bindRealApplication() throws Exception {
        if (mIsBindReal) {
            return;
        }
        // 如果用户(使用这个库的开发者)没有配置Application 就不用管了
        if (TextUtils.isEmpty(mAppName)) {
            return;
        }
        // 这个就是attachBaseContext传进来的ContextImpl
        Context baseContext = getBaseContext();
        // 反射创建出真实的用户配置的Application
        Class<?> delegateClass = Class.forName(mAppName);
        mDelegate = (Application) delegateClass.newInstance();
        // 反射获得attach方法
        Method attach = Application.class.getDeclaredMethod("attach", Context.class);
        // 设置允许访问
        attach.setAccessible(true);
        attach.invoke(mDelegate, baseContext);

        /**
         *  替换
         *  ContextImpl -> mOuterContext StubApp->MyApplication
         */
        Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
        // 获得mOuterContext属性
        Field mOuterContextField = contextImplClass.getDeclaredField("mOuterContext");
        mOuterContextField.setAccessible(true);
        mOuterContextField.set(baseContext, mDelegate);

        /**
         * ActivityThread、mAllApplications与mInitialApplication
         */
        // 获得ActivityThread：对象ActivityThread可以通过ContextImpl的mMainThread属性获得
        Field mMainThreadField = contextImplClass.getDeclaredField("mMainThread");
        mMainThreadField.setAccessible(true);
        Object mMainThread = mMainThreadField.get(baseContext);

        // 替换mInitialApplication
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Field mInitialApplicationField = activityThreadClass.getDeclaredField
                ("mInitialApplication");
        mInitialApplicationField.setAccessible(true);
        mInitialApplicationField.set(mMainThread, mDelegate);

        // 替换mAllApplications
        Field mAllApplicationsField = activityThreadClass.getDeclaredField
                ("mAllApplications");
        mAllApplicationsField.setAccessible(true);
        ArrayList<Application> mAllApplications =
                (ArrayList<Application>) mAllApplicationsField.get(mMainThread);
        mAllApplications.remove(this);
        mAllApplications.add(mDelegate);

        /**
         * LoadedApk -> mApplication StubApp
         */
        // LoadedApk可以通过ContextImpl的mPackageInfo属性获得
        Field mPackageInfoField = contextImplClass.getDeclaredField("mPackageInfo");
        mPackageInfoField.setAccessible(true);
        Object mPackageInfo = mPackageInfoField.get(baseContext);

        Class<?> loadedApkClass = Class.forName("android.app.LoadedApk");
        Field mApplicationField = loadedApkClass.getDeclaredField("mApplication");
        mApplicationField.setAccessible(true);
        mApplicationField.set(mPackageInfo, mDelegate);

        // 修改ApplicationInfo className LoadedApk
        Field mApplicationInfoField = loadedApkClass.getDeclaredField("mApplicationInfo");
        mApplicationInfoField.setAccessible(true);
        ApplicationInfo mApplicationInfo = (ApplicationInfo) mApplicationInfoField.get(mPackageInfo);
        mApplicationInfo.className = mAppName;

        mDelegate.onCreate();
        mIsBindReal = true;
    }

    @Override
    public String getPackageName() {
        // 如果meta-data设置了application
        // 让ContentProvider创建的时候使用的上下文在ActivityThread中的installProvider函数
        //命中else
        if (!TextUtils.isEmpty(mAppName)) {
            return "";
        }
        return super.getPackageName();
    }

    @Override
    public Context createPackageContext(String packageName,
                                        int flags) throws PackageManager.NameNotFoundException {
        if (TextUtils.isEmpty(mAppName)) {
            return super.createPackageContext(packageName, flags);
        }
        try {
            bindRealApplication();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mDelegate;
    }

    /**
     * 加载dex文件集合
     *
     * @param dexFiles
     */
    private void loadDex(List<File> dexFiles, File optimizedDirectory) throws
            NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        /**
         * 1.获得系统classloader中的dexElements数组
         */
        // 1.1 获得classloader中的pathList => DexPathList
        Field pathListField = DecryptUtils.findField(getClassLoader(), "pathList");
        Object pathList = pathListField.get(getClassLoader());
        // 1.2 获得pathList类中的dexElements
        Field dexElementsField = DecryptUtils.findField(pathList, "dexElements");
        Object[] dexElements = (Object[]) dexElementsField.get(pathList);
        /**
         * 2.创建新的element数组——解密后加载dex
         */
        // 需要适配
        Method makeDexElements;
        Object[] addElements;
        ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // 5.x
            makeDexElements = DecryptUtils.findMethod(pathList, "makeDexElements", ArrayList.class,
                    File.class, ArrayList.class);
            addElements = (Object[]) makeDexElements.invoke(pathList,
                    dexFiles,
                    optimizedDirectory,
                    suppressedExceptions);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 6.x
            makeDexElements = DecryptUtils.findMethod(pathList,
                    "makePathElements",
                    List.class,
                    File.class,
                    List.class);
            addElements = (Object[]) makeDexElements.invoke(pathList,
                    dexFiles,
                    optimizedDirectory,
                    suppressedExceptions);
        } else {
            makeDexElements = DecryptUtils.findMethod(pathList,
                    "makeDexElements",
                    List.class,
                    File.class,
                    List.class,
                    ClassLoader.class);
            Field definingContextField = DecryptUtils.findField(pathList, "definingContext");
            ClassLoader definingContext = (ClassLoader) definingContextField.get(pathList);
            addElements = (Object[]) makeDexElements.invoke(pathList,
                    dexFiles,
                    optimizedDirectory,
                    suppressedExceptions,
                    definingContext);
        }

        /**
         * 3.合并两个数组
         */
        // 创建一个数组
        Object[] newElements = (Object[]) Array.newInstance(dexElements.getClass().getComponentType(),
                dexElements.length + addElements.length);
        System.arraycopy(dexElements, 0, newElements, 0, dexElements.length);
        System.arraycopy(addElements, 0, newElements, dexElements.length, addElements.length);
        /**
         * 4.替换classloader中的 element数组
         */
        dexElementsField.set(pathList, newElements);
    }

    public void getMetaData() {
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo
                    (getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;
            // 是否设置app_name与app_version
            if (metaData != null) {
                // 是否存在name为app_name的meta-data数据
                if (metaData.containsKey("app_name")) {
                    mAppName = metaData.getString("app_name");
                }
                if (metaData.containsKey("app_version")) {
                    mAppVersion = metaData.getString("app_version");
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}