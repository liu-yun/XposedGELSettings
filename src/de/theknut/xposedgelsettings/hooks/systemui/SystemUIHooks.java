package de.theknut.xposedgelsettings.hooks.systemui;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.theknut.xposedgelsettings.hooks.Common;
import de.theknut.xposedgelsettings.hooks.HooksBaseClass;
import de.theknut.xposedgelsettings.hooks.PreferencesHelper;

public class SystemUIHooks extends HooksBaseClass {
	
	static ActivityManager activityManager = null;
	
	public static void initAllHooks(LoadPackageParam lpparam) {
		
		if (!PreferencesHelper.hideClock && !PreferencesHelper.dynamicBackbutton && !PreferencesHelper.dynamicHomebutton) 
		{
			return;
		}
		
		final Class<?> WorkspaceClass = findClass(Common.WORKSPACE, lpparam.classLoader);
		final Class<?> PagedViewClass = findClass(Common.PAGED_VIEW, lpparam.classLoader);
		final Class<?> LauncherClass = findClass(Common.LAUNCHER, lpparam.classLoader);		
		
		XposedBridge.hookAllMethods(PagedViewClass, "snapToPage", new XC_MethodHook() {
			
			boolean gnow = true;
			
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				
				gnow = (Boolean) callMethod(Common.LAUNCHER_INSTANCE, "hasCustomContentToLeft");
				
				try {
					if (!((Boolean) callMethod(Common.LAUNCHER_INSTANCE, "isAllAppsVisible"))
						&& getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")
						&& getObjectField(param.thisObject, "mState").toString().equals("NORMAL")) {
						
						Intent myIntent = new Intent();
						myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
						
						if ((Integer) param.args[0] == (PreferencesHelper.defaultHomescreen - 1)) {
							
							myIntent.putExtra(Common.XGELS_ACTION, "ON_DEFAULT_HOMESCREEN");
							myIntent.setAction(Common.XGELS_INTENT);
							Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
						}
						else if ((Integer) param.args[0] == 0 && gnow) {
							
							if (PreferencesHelper.dynamicBackButtonOnEveryScreen) {
								myIntent.putExtra(Common.XGELS_ACTION, "HOME_ORIG");
							} else {
								myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
							}
							
							myIntent.setAction(Common.XGELS_INTENT);
							Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
						}
					}
				} catch (Throwable e) { }
			}
		});
		
		XposedBridge.hookAllMethods(WorkspaceClass, "onPageBeginMoving", new XC_MethodHook() {
			
			int TOUCH_STATE_REST = 0;
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {				
				if (DEBUG) log("SystemUIHooks: onPageBeginMoving TouchState " + getObjectField(Common.WORKSPACE_INSTANCE, "mTouchState"));
				
				int currentPage = getIntField(param.thisObject, "mCurrentPage");
				
				if (!((Boolean) callMethod(Common.LAUNCHER_INSTANCE, "isAllAppsVisible"))
					&& getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")
					&& getIntField(Common.WORKSPACE_INSTANCE, "mTouchState") != TOUCH_STATE_REST
					&& getIntField(Common.WORKSPACE_INSTANCE, "mNextPage") != currentPage) {
					
											
					Intent myIntent = new Intent();
					myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
					
					if (currentPage == (PreferencesHelper.defaultHomescreen - 1)) {
						
						if (PreferencesHelper.dynamicBackButtonOnEveryScreen) {
							myIntent.putExtra(Common.XGELS_ACTION, "HOME_ORIG");
						} else {
							myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
						}
						
						myIntent.setAction(Common.XGELS_INTENT);
						Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
					}
				}
			}
		});
		
		XposedBridge.hookAllMethods(WorkspaceClass, "onPageEndMoving", new XC_MethodHook() {
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {				
				if (DEBUG) log("SystemUIHooks: onPageEndMoving TouchState" + getObjectField(Common.WORKSPACE_INSTANCE, "mTouchState"));
				
				if (!((Boolean) callMethod(Common.LAUNCHER_INSTANCE, "isAllAppsVisible"))
					&& getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")) {
					
					int currentPage = getIntField(param.thisObject, "mCurrentPage");						
					Intent myIntent = new Intent();
					myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
					
					if (currentPage != (PreferencesHelper.defaultHomescreen - 1)) {
						if (PreferencesHelper.dynamicBackButtonOnEveryScreen) {
							myIntent.putExtra(Common.XGELS_ACTION, "HOME_ORIG");
						} else {
							myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
						}
						
						myIntent.setAction(Common.XGELS_INTENT);
						Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
					}
				}
			}
		});
		
		XposedBridge.hookAllMethods(WorkspaceClass, "enterOverviewMode", new XC_MethodHook() {
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (DEBUG) log("SystemUIHooks: enterOverviewMode");
				
				Intent myIntent = new Intent();
				myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
				myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
				myIntent.setAction(Common.XGELS_INTENT);
				Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
			}
		});
		
		XposedBridge.hookAllMethods(LauncherClass, "onPause", new XC_MethodHook() {
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {				
				if (DEBUG) log("SystemUIHooks: onPause");
				
				if (getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")) {
					Intent myIntent = new Intent();
					myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
					myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");						
					myIntent.setAction(Common.XGELS_INTENT);
					Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
				}
			}
		});
		
		XposedBridge.hookAllMethods(LauncherClass, "onStop", new XC_MethodHook() {
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {				
				if (DEBUG) log("SystemUIHooks: onStop isPaused " + getBooleanField(param.thisObject, "mPaused"));
				
				if (getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")) {
					
					Intent myIntent = new Intent();
					myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
					myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");						
					myIntent.setAction(Common.XGELS_INTENT);
					Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
				}
			}			
		});

		XposedBridge.hookAllMethods(LauncherClass, "onStart", new XC_MethodHook() {
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {				
				if (DEBUG) log("SystemUIHooks: onStart");
				
				boolean isDefaultHomescreen = getIntField(Common.WORKSPACE_INSTANCE, "mCurrentPage") == (PreferencesHelper.defaultHomescreen - 1);
				
				if (activityManager == null) {
					activityManager = (ActivityManager) Common.LAUNCHER_CONTEXT.getSystemService(Context.ACTIVITY_SERVICE);
				}
				
				List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
								
				if (!(Boolean) callMethod(param.thisObject, "isAllAppsVisible")
					&& isDefaultHomescreen
					&& Common.PACKAGE_NAMES.contains(appProcesses.get(0).processName.replace(":search", ""))
					&& getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")) {
					
					Intent myIntent = new Intent();
					myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
					myIntent.putExtra(Common.XGELS_ACTION, "ON_DEFAULT_HOMESCREEN");						
					myIntent.setAction(Common.XGELS_INTENT);
					Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
				}				
			}
		});
		
		XposedBridge.hookAllMethods(LauncherClass, "onResume", new XC_MethodHook() {
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {				
				if (DEBUG) log("SystemUIHooks: onResume currentPage" + getIntField(Common.WORKSPACE_INSTANCE, "mCurrentPage"));
				
				if (activityManager == null) {
					activityManager = (ActivityManager) Common.LAUNCHER_CONTEXT.getSystemService(Context.ACTIVITY_SERVICE);
				}
				
				if (!((Boolean) callMethod(param.thisObject, "isAllAppsVisible"))
					&& !(Boolean) callMethod(Common.WORKSPACE_INSTANCE, "isOnOrMovingToCustomContent")
					&& getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")) {
					
					int currentPage = getIntField(Common.WORKSPACE_INSTANCE, "mCurrentPage");
					
					if (currentPage == (PreferencesHelper.defaultHomescreen - 1)) {
						
						Intent myIntent = new Intent();
						myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
						myIntent.putExtra(Common.XGELS_ACTION, "ON_DEFAULT_HOMESCREEN");						
						myIntent.setAction(Common.XGELS_INTENT);
						Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
					}
				}
			}
		});
		
		XposedBridge.hookAllMethods(LauncherClass, "onWorkspaceShown", new XC_MethodHook() {
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (DEBUG) log("SystemUIHooks: onWorkspaceShown");
				
				int currentPage = getIntField(Common.WORKSPACE_INSTANCE, "mCurrentPage");
				
				if (activityManager == null) {
					activityManager = (ActivityManager) Common.LAUNCHER_CONTEXT.getSystemService(Context.ACTIVITY_SERVICE);
				}
				
				List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
				
				if (DEBUG) log("SystemUIHooks: paused " + getBooleanField(param.thisObject, "mPaused") + " onWorkspaceShown defaultscreen " + (PreferencesHelper.defaultHomescreen - 1) + " " + appProcesses.get(0).processName);
				if (DEBUG) log("SystemUIHooks: isInOverViewMode " + callMethod(Common.WORKSPACE_INSTANCE, "isInOverviewMode"));
				
				if (currentPage == (PreferencesHelper.defaultHomescreen - 1)
					&& Common.PACKAGE_NAMES.contains(appProcesses.get(0).processName.replace(":search", ""))
					&& !getBooleanField(param.thisObject, "mPaused")
					&& !getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("OVERVIEW")) {	
					
					Intent myIntent = new Intent();
					myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
					myIntent.putExtra(Common.XGELS_ACTION, "ON_DEFAULT_HOMESCREEN");						
					myIntent.setAction(Common.XGELS_INTENT);
					Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
				}
			}
		});
		
		XposedBridge.hookAllMethods(LauncherClass, "moveToCustomContentScreen", new XC_MethodHook() {
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {				
				if (DEBUG) log("SystemUIHooks: moveToCustomContentScreen");
				
				Intent myIntent = new Intent();
				myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
				myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");						
				myIntent.setAction(Common.XGELS_INTENT);
				Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
			}
		});
		
		XposedBridge.hookAllMethods(LauncherClass, "showAllApps", new XC_MethodHook() {
			
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (DEBUG) log("SystemUIHooks: showAllApps");
				
				Intent myIntent = new Intent();
				myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
				myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");						
				myIntent.setAction(Common.XGELS_INTENT);
				Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
			}
		});
		
		if (PreferencesHelper.dynamicBackbutton) {
			
			if (PreferencesHelper.dynamicIconBackbutton) {
				
				XposedBridge.hookAllMethods(LauncherClass, "openFolder", new XC_MethodHook() {
					
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {						
						if (DEBUG) log("SystemUIHooks: openFolder");
						
						int currentPage = getIntField(Common.WORKSPACE_INSTANCE, "mCurrentPage");
						
						if (currentPage == (PreferencesHelper.defaultHomescreen - 1)
							|| PreferencesHelper.dynamicBackButtonOnEveryScreen) {					
							Intent myIntent = new Intent();
							myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
							myIntent.putExtra(Common.XGELS_ACTION, "BACK_ORIG");						
							myIntent.setAction(Common.XGELS_INTENT);
							Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
						}
					}
				});
				
				XposedBridge.hookAllMethods(LauncherClass, "closeFolder", new XC_MethodHook() {
					
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (DEBUG) log("SystemUIHooks: closeFolder");
						
						int currentPage = getIntField(Common.WORKSPACE_INSTANCE, "mCurrentPage");
						
						if (callMethod(Common.WORKSPACE_INSTANCE, "getOpenFolder") != null
							&& (currentPage == (PreferencesHelper.defaultHomescreen - 1) || PreferencesHelper.dynamicBackButtonOnEveryScreen)
							&& !getBooleanField(param.thisObject, "mPaused")
							&& getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")) {
							
							Intent myIntent = new Intent();
							myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
							myIntent.putExtra(Common.XGELS_ACTION, "BACK_POWER_OFF");						
							myIntent.setAction(Common.XGELS_INTENT);
							Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
						}
					}
				});
			}
			
			XposedBridge.hookAllMethods(LauncherClass, "onBackPressed", new XC_MethodHook() {
				
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					boolean isDefaultHomescreen = (getIntField(Common.WORKSPACE_INSTANCE, "mCurrentPage") == (PreferencesHelper.defaultHomescreen - 1))
							|| (getIntField(Common.WORKSPACE_INSTANCE, "mNextPage") == (PreferencesHelper.defaultHomescreen - 1));
					
					if (DEBUG) log("SystemUIHooks: onBackPressed " + (PreferencesHelper.defaultHomescreen - 1) + " : " + isDefaultHomescreen);
					
					if (!((Boolean) callMethod(param.thisObject, "isAllAppsVisible"))
						&& callMethod(Common.WORKSPACE_INSTANCE, "getOpenFolder") == null
						&& (isDefaultHomescreen || PreferencesHelper.dynamicBackButtonOnEveryScreen)
						&& getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")) {
						
						Intent myIntent = new Intent();
						myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_OTHER);
						myIntent.putExtra(Common.XGELS_ACTION, "GO_TO_SLEEP");						
						myIntent.setAction(Common.XGELS_INTENT);
						Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
					}
				}
			});
		}
		
		if (PreferencesHelper.dynamicHomebutton) {
			
			XposedBridge.hookAllMethods(LauncherClass, "onNewIntent", new XC_MethodHook() {
				
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {					
					if (DEBUG) log("SystemUIHooks: onNewIntent");
					
					if (Intent.ACTION_MAIN.equals(((Intent)param.args[0]).getAction())
						&& !((Boolean) callMethod(param.thisObject, "isAllAppsVisible"))
						&& callMethod(Common.WORKSPACE_INSTANCE, "getOpenFolder") == null) {
						
						int currentPage = getIntField(Common.WORKSPACE_INSTANCE, "mCurrentPage");
						
						if ((currentPage == (PreferencesHelper.defaultHomescreen - 1)
								|| getIntField(Common.WORKSPACE_INSTANCE, "mNextPage") == (PreferencesHelper.defaultHomescreen - 1))
							&& getBooleanField(param.thisObject, "mHasFocus")
							&& getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")) {
							
							callMethod(Common.LAUNCHER_INSTANCE, "showAllApps", true, Common.CONTENT_TYPE, !PreferencesHelper.appdrawerRememberLastPosition);
							param.setResult(null);
						}
					}
				}
			});
		}	
	}
}