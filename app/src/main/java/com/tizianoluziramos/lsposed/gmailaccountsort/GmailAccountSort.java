package com.tizianoluziramos.lsposed.gmailaccountsort;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class GmailAccountSort implements IXposedHookLoadPackage {
    private static final String GMAIL = "com.google.android.gm";
    private enum GmailBuild {
        NOVEMBER_2("2026.11.02.1015914183.Release", "axcw", "chkk", "awzy",
                "axhx", "axtv", "axuf", "cnim", "cnli", "cnhu", "cngv",
                null, "cnki", "axid", "axjr"),
        NOVEMBER_9("2026.11.09.1019722178.Release", "axfu", "chsg", "axcv",
                "axku", "axws", "axxc", "cnqx", "cntv", "cnqf", "cnpg",
                "cnsu", "cnsv", "axla", "axmp");

        final String version;
        final String classicAdapter;
        final String immutableList;
        final String accountConverter;
        final String bentoAdapter;
        final String bentoAdapterBase;
        final String rowFactory;
        final String row;
        final String rowIdentifier;
        final String accountMetadata;
        final String trailingCard;
        final String trailingContent;
        final String accountState;
        final String accountBinder;
        final String sectionBinder;

        GmailBuild(String version, String classicAdapter, String immutableList,
                String accountConverter, String bentoAdapter, String bentoAdapterBase,
                String rowFactory, String row, String rowIdentifier, String accountMetadata,
                String trailingCard, String trailingContent, String accountState,
                String accountBinder, String sectionBinder) {
            this.version = version;
            this.classicAdapter = classicAdapter;
            this.immutableList = immutableList;
            this.accountConverter = accountConverter;
            this.bentoAdapter = bentoAdapter;
            this.bentoAdapterBase = bentoAdapterBase;
            this.rowFactory = rowFactory;
            this.row = row;
            this.rowIdentifier = rowIdentifier;
            this.accountMetadata = accountMetadata;
            this.trailingCard = trailingCard;
            this.trailingContent = trailingContent;
            this.accountState = accountState;
            this.accountBinder = accountBinder;
            this.sectionBinder = sectionBinder;
        }

        static GmailBuild forVersion(String version) {
            if (version == null) {
                return null;
            }

            GmailBuild selected = null;
            for (GmailBuild build : values()) {
                if (compareVersions(version, build.version) >= 0 &&
                        (selected == null || compareVersions(build.version, selected.version) > 0)) {
                    selected = build;
                }
            }
            return selected;
        }

        private static int compareVersions(String a, String b) {
            String[] left = a.split("\\.");
            String[] right = b.split("\\.");
            if (left.length != 5 || right.length != 5 ||
                    !"Release".equals(left[4]) || !"Release".equals(right[4])) {
                return -1;
            }
            try {
                for (int i = 0; i < 4; i++) {
                    int comparison = Long.compare(Long.parseLong(left[i]), Long.parseLong(right[i]));
                    if (comparison != 0) {
                        return comparison;
                    }
                }
            } catch (NumberFormatException error) {
                return -1;
            }
            return 0;
        }

        Method findRowFactory(ClassLoader loader, Class<?> owner, Class<?> result)
                throws ReflectiveOperationException {
            Class<?> metadata = loader.loadClass(accountMetadata);
            Class<?> card = loader.loadClass(trailingCard);
            Class<?> state = loader.loadClass(accountState);
            if (trailingContent == null) {
                return method(owner, "h", result, int.class, metadata, card, state, boolean.class);
            }
            return method(owner, "h", result, int.class, metadata, card,
                    loader.loadClass(trailingContent), state, boolean.class);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (!GMAIL.equals(load.packageName) || !GMAIL.equals(load.processName)) {
            return;
        }

        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Context context = (Context) param.args[0];
                        if (!GMAIL.equals(context.getPackageName()) || param.hasThrowable()) {
                            return;
                        }
                        try {
                            PackageInfo info = context.getPackageManager().getPackageInfo(GMAIL, 0);
                            GmailBuild build = GmailBuild.forVersion(info.versionName);
                            if (build != null) {
                                log("Loading Gmail " + build.version);
                                installHooks(context.getClassLoader(), build);
                            } else {
                                log("Unsupported Gmail version: " + info.versionName);
                            }
                        } catch (PackageManager.NameNotFoundException error) {
                            log("Gmail version unavailable");
                        }
                    }
                });
    }

    private static void installHooks(ClassLoader loader, GmailBuild build) {
        try {
            hookClassicMenu(loader, build);
            log("Classic account selector ready");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            log("Classic account selector unavailable: " + error.getClass().getSimpleName());
        }

        try {
            hookBentoMenu(loader, build);
            log("Bento account selector ready");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            log("Bento account selector unavailable: " + error.getClass().getSimpleName());
        }
    }

    private static void hookClassicMenu(ClassLoader loader, GmailBuild build)
            throws ReflectiveOperationException {
        // The adapter builds and diffs the alternative-account rows. The converter returns the email.
        Class<?> adapter = loader.loadClass(build.classicAdapter);
        Class<?> immutableList = loader.loadClass(build.immutableList);
        Class<?> converter = loader.loadClass(build.accountConverter);
        Field accounts = field(adapter, "f", immutableList);
        Field accountConverter = field(adapter, "a", converter);
        Method email = method(converter, "f", String.class, Object.class);
        Method copyOf = method(immutableList, "i", immutableList, Collection.class);
        Method update = method(adapter, "f", void.class);

        XposedBridge.hookMethod(update, new XC_MethodHook() {
            private boolean reported;

            @Override
            protected void beforeHookedMethod(MethodHookParam param)
                    throws ReflectiveOperationException {
                List<?> original = (List<?>) accounts.get(param.thisObject);
                if (original == null) {
                    return;
                }
                Object actualConverter = accountConverter.get(param.thisObject);
                List<?> ordered = AccountOrder.byAddress(original,
                        account -> (String) email.invoke(actualConverter, account));
                if (ordered != original) {
                    accounts.set(param.thisObject, copyOf.invoke(null, ordered));
                    if (!reported) {
                        log("Reordered classic account rows");
                        reported = true;
                    }
                }
            }
        });
    }

    private static void hookBentoMenu(ClassLoader loader, GmailBuild build)
            throws ReflectiveOperationException {
        // The email can be hidden by the layout. Capture it as Gmail builds each row.
        // The row identifier survives copies of the UI model.
        Class<?> adapter = loader.loadClass(build.bentoAdapter);
        Class<?> adapterBase = loader.loadClass(build.bentoAdapterBase);
        Class<?> rowFactory = loader.loadClass(build.rowFactory);
        Class<?> row = loader.loadClass(build.row);
        Class<?> identifier = loader.loadClass(build.rowIdentifier);
        if (!adapterBase.isAssignableFrom(adapter)) {
            throw new NoSuchMethodException("Account adapter hierarchy");
        }

        Field rowIdentifier = field(row, "a", identifier);
        Field sourceEmail = field(rowFactory, "c", String.class);
        Method submit = method(adapterBase, "I", void.class, List.class);
        Method makeRow = build.findRowFactory(loader, rowFactory, row);
        Map<Object, String> addresses = Collections.synchronizedMap(new WeakHashMap<>());

        XposedBridge.hookMethod(makeRow, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
                    throws ReflectiveOperationException {
                Object result = param.getResult();
                if (row.isInstance(result)) {
                    Object id = rowIdentifier.get(result);
                    String address = (String) sourceEmail.get(param.thisObject);
                    if (id != null) {
                        addresses.put(id, address);
                    }
                }
            }
        });

        XposedBridge.hookMethod(submit, new XC_MethodHook() {
            private boolean reported;

            @Override
            protected void beforeHookedMethod(MethodHookParam param)
                    throws ReflectiveOperationException {
                if (param.thisObject.getClass() == adapter && param.args[0] instanceof List<?>) {
                    List<?> original = (List<?>) param.args[0];
                    List<?> ordered = AccountOrder.byAddress(original, account ->
                            row.isInstance(account) ? addresses.get(rowIdentifier.get(account)) : null);
                    param.args[0] = ordered;
                    if (ordered != original && !reported) {
                        log("Reordered Bento account rows");
                        reported = true;
                    }
                }
            }
        });

        // The short submit method can be inlined into either binding.
        deoptimizeBinding(loader, build.accountBinder);
        deoptimizeBinding(loader, build.sectionBinder);
    }

    private static void deoptimizeBinding(ClassLoader loader, String binder) {
        try {
            Method deoptimize = XposedBridge.class.getMethod("deoptimizeMethod", Member.class);
            Method bind = method(loader.loadClass(binder), "b", void.class,
                    Object.class, Object.class);
            deoptimize.invoke(null, bind);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            log("Optional deoptimization unavailable for " + binder);
        }
    }

    private static Field field(Class<?> owner, String name, Class<?> type)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        if (field.getType() != type) {
            throw new NoSuchFieldException(owner.getName() + "." + name);
        }
        field.setAccessible(true);
        return field;
    }

    private static Method method(Class<?> owner, String name, Class<?> returns, Class<?>... parameters)
            throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, parameters);
        if (method.getReturnType() != returns) {
            throw new NoSuchMethodException(owner.getName() + "." + name);
        }
        method.setAccessible(true);
        return method;
    }

    private static void log(String message) {
        XposedBridge.log("[GmailAccountSort] " + message);
    }
}
