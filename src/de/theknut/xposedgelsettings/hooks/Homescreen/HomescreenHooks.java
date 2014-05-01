package de.theknut.xposedgelsettings.hooks.homescreen;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import de.theknut.xposedgelsettings.hooks.Common;
import de.theknut.xposedgelsettings.hooks.HooksBaseClass;
import de.theknut.xposedgelsettings.hooks.PreferencesHelper;
import de.theknut.xposedgelsettings.hooks.appdrawer.AppsCustomizeLayoutConstructorHook;
import de.theknut.xposedgelsettings.hooks.appdrawer.OnTabChangedHook;
import de.theknut.xposedgelsettings.hooks.general.MoveToDefaultScreenHook;
import de.theknut.xposedgelsettings.hooks.systemui.SystemUIHooks;

public class HomescreenHooks extends HooksBaseClass {

	public static void initAllHooks(LoadPackageParam lpparam) {
		
		// save the workspace instance
		final Class<?> WorkspaceClass = findClass(Common.WORKSPACE, lpparam.classLoader);
		XposedBridge.hookAllConstructors(WorkspaceClass, new WorkspaceConstructorHook());
		
		// change the default homescreen
		XposedBridge.hookAllMethods(WorkspaceClass, "moveToDefaultScreen", new MoveToDefaultScreenHook());
		
		// don't animate background to semitransparent
		// XposedBridge.hookAllMethods(WorkspaceClass, "animateBackgroundGradient", new AnimateBackgroundGradientHook());
		
		final Class<?> DeviceProfileClass = findClass(Common.DEVICE_PROFILE, lpparam.classLoader);		
		// modify homescreen grid
		XposedBridge.hookAllConstructors(DeviceProfileClass, new DeviceProfileConstructorHook());
		
		if (PreferencesHelper.iconSettingsSwitchHome || PreferencesHelper.homescreenFolderSwitch || PreferencesHelper.appdockSettingsSwitch) {			
			final Class<?> CellLayoutClass = findClass(Common.CELL_LAYOUT, lpparam.classLoader);
			// changing the appearence of the icons on the homescreen
			XposedBridge.hookAllMethods(CellLayoutClass, "addViewToCellLayout", new AddViewToCellLayoutHook());
		}
		
		final Class<?> AppsCustomizePagedViewClass = findClass(Common.APPS_CUSTOMIZE_PAGED_VIEW, lpparam.classLoader);
		XposedBridge.hookAllConstructors(AppsCustomizePagedViewClass, new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				// saving the content type
				Common.CONTENT_TYPE = getObjectField(param.thisObject, "mContentType");
			};
		});
		
		if (PreferencesHelper.continuousScroll) {
			
			// over scroll to app drawer or first page
			XposedBridge.hookAllMethods(WorkspaceClass, "overScroll", new OverScrollWorkspaceHook());
			
			final Class<?> LauncherClass = findClass(Common.LAUNCHER, lpparam.classLoader);
			XposedBridge.hookAllMethods(LauncherClass, "onWorkspaceShown", new OnWorkspaceShownHook());
		}
		
		if (PreferencesHelper.appdockSettingsSwitch || PreferencesHelper.changeGridSizeHome) {
			
			// hide the app dock
			XposedBridge.hookAllMethods(DeviceProfileClass, "getWorkspacePadding", new GetWorkspacePaddingHook());
			
			if (PreferencesHelper.appdockSettingsSwitch) {
				final Class<?> HotseatClass = findClass(Common.LAUNCHER3 + "Hotseat", lpparam.classLoader);
				XposedBridge.hookAllConstructors(HotseatClass, new HotseatConstructorHook());
			}
		}
		
		final Class<?> LauncherClass = findClass(Common.LAUNCHER, lpparam.classLoader);
		if (Common.HOOKED_PACKAGE.equals(Common.TREBUCHET_PACKAGE)) {
			// move to default homescreen after workspace has finished loading
			XposedBridge.hookAllMethods(LauncherClass, "onFinishBindingItems", new FinishBindingItemsHook());
		}
		else {
			// move to default homescreen after workspace has finished loading
			XposedBridge.hookAllMethods(LauncherClass, "finishBindingItems", new FinishBindingItemsHook());
		}		
		
		SystemUIHooks.initAllHooks(lpparam);
	}
}
