package de.theknut.xposedgelsettings.hooks.gestures;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.FrameLayout.LayoutParams;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import de.theknut.xposedgelsettings.hooks.Common;
import de.theknut.xposedgelsettings.hooks.PreferencesHelper;

public class GestureHooks extends GestureHelper {
	
	static boolean autoHideAppDock = PreferencesHelper.hideAppDock && PreferencesHelper.autoHideAppDock;
	static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	static ScheduledFuture<?> delayedTask = null;
	static long lastTouchTime = 0;
	static long currTouchTime = 0;
	static Object lastTouchView = null;
	static Object instance = null;
	static boolean isScheduledOrRunning = false;
	
	public static void initAllHooks(LoadPackageParam lpparam) {
		
		final Class<?> WorkspaceClass = findClass(Common.WORKSPACE, lpparam.classLoader);
		
		if (PreferencesHelper.appdockSettingsSwitch && autoHideAppDock) {
			final Class<?> LauncherClass = findClass(Common.LAUNCHER, lpparam.classLoader);
			final Class<?> FolderClass = findClass(Common.FOLDER, lpparam.classLoader);
			
			XposedBridge.hookAllMethods(LauncherClass, "showHotseat", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.setResult(null);
				}
			});
			
			XposedBridge.hookAllMethods(LauncherClass, "onTransitionPrepare", new XC_MethodHook() {
				
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (DEBUG) log("GestureHooks: onTransitionPrepare");
					hideAppdock(0);
				}
			});
			
			XposedBridge.hookAllMethods(LauncherClass, "onResume", new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (DEBUG) log("GestureHooks: onPause");
					
						hideAppdock(FORCEHIDE);
				}
			});
			
			XposedBridge.hookAllMethods(LauncherClass, "hideAppsCustomizeHelper", new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (DEBUG) log("GestureHooks: onPause");
					
						hideAppdock(FORCEHIDE);
				}
			});
			
			XposedBridge.hookAllMethods(WorkspaceClass, "onWindowVisibilityChanged", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (DEBUG) log("GestureHooks: onWindowVisibilityChanged");
					
					try {
						if (mHotseat.getAlpha() != 1.0f) {
							
							if (autoHideAppDock) {
								if (DEBUG) log("GestureHooks: onWindowVisibilityChanged autoHideAppDock");
								hideAppdock(0);
							} else {
								if (DEBUG) log("GestureHooks: onWindowVisibilityChanged !autoHideAppDock");
								hideAppdock(0);
								showAppdock(0);
							}
						}
					} catch (Exception ex) {
						log(ex.getMessage());
					}
				}
			});
			
			XposedBridge.hookAllMethods(WorkspaceClass, "onRequestFocusInDescendants", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (DEBUG) log("GestureHooks: onRequestFocusInDescendants");
					
					try {
						if (mHotseat.getAlpha() == 1.0f) {
							
							if (autoHideAppDock) {
								if (DEBUG) log("GestureHooks: onRequestFocusInDescendants autoHideAppDock");
								hideAppdock(200);
							} else {
								if (DEBUG) log("GestureHooks: onRequestFocusInDescendants !autoHideAppDock");
								hideAppdock(0);
								showAppdock(0);
							}
						}
					} catch (Exception ex) {
						log(ex.getMessage());
					}
				}
			});
		}
		
		if (true) {
			XposedBridge.hookAllMethods(WorkspaceClass, "onInterceptTouchEvent", new XC_MethodHook() {
				
				boolean gnow = true;
				float downY = 0, downX = 0;
				
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (wm == null) {
						init();
						gnow = (Boolean) callMethod(Common.LAUNCHER_INSTANCE, "hasCustomContentToLeft");
					}
					
					final int currentPage = getIntField(Common.WORKSPACE_INSTANCE, "mCurrentPage");					
					if (currentPage == 0 && gnow) return;					
					
					MotionEvent ev = (MotionEvent) param.args[0];
					
					int rotation = Common.LAUNCHER_CONTEXT.getResources().getConfiguration().orientation;
					switch (rotation) {
						case Configuration.ORIENTATION_PORTRAIT:
							if (isLandscape) {
								if (PreferencesHelper.appdockSettingsSwitch && PreferencesHelper.hideAppDock) { 
									hideAppdock(0);
								}
								
								init();
							}
							
							isLandscape = false;
							break;
						case Configuration.ORIENTATION_LANDSCAPE:
							if (!isLandscape) {
								if (PreferencesHelper.appdockSettingsSwitch && PreferencesHelper.hideAppDock) { 
									hideAppdock(0);
								}
								
								init();
							}
							
							isLandscape = true;
							break;
						default: break;
					}
					
					switch (ev.getAction() & MotionEvent.ACTION_MASK) {
						case MotionEvent.ACTION_DOWN:
							downY = ev.getRawY();
							downX = ev.getRawX();
							
							mHotseat = (View) getObjectField(Common.LAUNCHER_INSTANCE, "mHotseat");
							
							if (PreferencesHelper.appdockSettingsSwitch && PreferencesHelper.hideAppDock) {
								LayoutParams lp = (LayoutParams) mHotseat.getLayoutParams();
								if (mHotseat.getAlpha() == 1.0f
									&& (lp.width == 0 || lp.height == 0)) {

									mHotseat.setAlpha(0.0f);
									lp.width = lp.width = 0;
									mHotseat.setLayoutParams(lp);
									
								} else if (autoHideAppDock) {
									
									hideAppdock(animateDuration);
								}
							}
							
							break;
						case MotionEvent.ACTION_UP:
							
							switch (identifyGesture(ev.getRawX(), ev.getRawY(), downX, downY)) {
								case DOWN_LEFT:
									handleGesture(getGestureKey(Gestures.DOWN_LEFT), PreferencesHelper.gesture_one_down_left);
									break;
								case DOWN_MIDDLE:
									handleGesture(getGestureKey(Gestures.DOWN_MIDDLE), PreferencesHelper.gesture_one_down_middle);
									break;
								case DOWN_RIGHT:
									handleGesture(getGestureKey(Gestures.DOWN_RIGHT), PreferencesHelper.gesture_one_down_right);
									break;
								case UP_LEFT:
									handleGesture(getGestureKey(Gestures.UP_LEFT), PreferencesHelper.gesture_one_up_left);
									break;
								case UP_MIDDLE:
									handleGesture(getGestureKey(Gestures.UP_MIDDLE), PreferencesHelper.gesture_one_up_middle);
									break;
								case UP_RIGHT:
									handleGesture(getGestureKey(Gestures.UP_RIGHT), PreferencesHelper.gesture_one_up_right);
									break;
								default:
									break;
							}
							
							break;
						case MotionEvent.ACTION_MOVE:
							break;
						default:
							break;
					}
				}
			});
		}
		
		if (PreferencesHelper.gesture_appdrawer) {
			XC_MethodHook gestureHook = new XC_MethodHook() {
				
				void init() throws IOException {
					wm = (WindowManager) Common.LAUNCHER_CONTEXT.getSystemService(Context.WINDOW_SERVICE);
					display = wm.getDefaultDisplay();
					size = new Point();
					display.getSize(size);
					width = size.x;
					height = size.y;
				}
				
				float downY;
				boolean isDown = false;
				
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					if (wm == null) init();
					
					MotionEvent ev = (MotionEvent) param.args[0];
					
					int rotation = Common.LAUNCHER_CONTEXT.getResources().getConfiguration().orientation;
					switch (rotation) {
						case Configuration.ORIENTATION_PORTRAIT:
							if (isLandscape) {
								init();
							}
							
							isLandscape = false;
							break;
						case Configuration.ORIENTATION_LANDSCAPE:
							if (!isLandscape) {
								init();
							}
							
							isLandscape = true;
							break;
						default: break;
					}
					
					switch (ev.getAction() & MotionEvent.ACTION_MASK) {
					
						case MotionEvent.ACTION_MOVE:
							if (DEBUG) log("MOVE: " + ev.getRawY());
							
							if (!isDown) {
								downY = ev.getRawY();
							}
							
							break;
						case MotionEvent.ACTION_DOWN:
							if (DEBUG) log("DOWN: " + ev.getRawY());
							
							downY = ev.getRawY();
							isDown = true;
							break;
						case MotionEvent.ACTION_UP:
							if (DEBUG) log("UP: " + ev.getRawY());
							
							isDown = false;
							if ((ev.getRawY() - downY) > (height / 6)) {
								
								callMethod(getObjectField(param.thisObject, "mLauncher"), "showWorkspace", true);
								
							} else if ((ev.getRawY() - downY) < -(height / 6)) {
								
								if (Common.HOOKED_PACKAGE.equals(Common.TREBUCHET_PACKAGE)) {
									Toast.makeText(Common.LAUNCHER_CONTEXT, "XGELS: Unfortunately swipe up to toggle apps/widgets doesn't work on Trebuchet", Toast.LENGTH_LONG).show();
									return;
								}
								
								Object tabhost = getObjectField(Common.LAUNCHER_INSTANCE, "mAppsCustomizeTabHost");
								
								if (!getBooleanField(tabhost, "mInTransition")) {
								
									String tabtag = (String) callMethod(tabhost, "getCurrentTabTag");
									if (tabtag.equals("APPS")) {
										
										callMethod(getObjectField(param.thisObject, "mLauncher"), "showWorkspace", false);
										Object ContentType = callMethod(tabhost, "getContentTypeForTabTag", "WIDGETS");
										callMethod(Common.LAUNCHER_INSTANCE, "showAllApps", true, ContentType, !PreferencesHelper.appdrawerRememberLastPosition);
										
									} else if (tabtag.equals("WIDGETS")) {
										
										callMethod(getObjectField(param.thisObject, "mLauncher"), "showWorkspace", false);
										Object ContentType = callMethod(tabhost, "getContentTypeForTabTag", "APPS");
										callMethod(Common.LAUNCHER_INSTANCE, "showAllApps", true, ContentType, !PreferencesHelper.appdrawerRememberLastPosition);
										
									}
								}
							}
							
							break;
						default:
							break;
					}
				}
			};
			
			if (Common.HOOKED_PACKAGE.equals(Common.TREBUCHET_PACKAGE)) {
				
				final Class<?> PagedViewWithDraggableItemsClass = findClass(Common.PAGED_VIEW_WITH_DRAGGABLE_ITEMS, lpparam.classLoader);
				XposedBridge.hookAllMethods(PagedViewWithDraggableItemsClass, "onTouchEvent", gestureHook);
				XposedBridge.hookAllMethods(PagedViewWithDraggableItemsClass, "onInterceptTouchEvent", gestureHook);
				
			}
			else if (Common.HOOKED_PACKAGE.equals(Common.GEL_PACKAGE)) {
				
				final Class<?> PagedViewWithDraggableItemsClass = findClass(Common.PAGED_VIEW_WITH_DRAGGABLE_ITEMS, lpparam.classLoader);
				XposedBridge.hookAllMethods(PagedViewWithDraggableItemsClass, "onTouchEvent", gestureHook);
				XposedBridge.hookAllMethods(PagedViewWithDraggableItemsClass, "onInterceptTouchEvent", gestureHook);
				
			}
		}
		
		if (!PreferencesHelper.gesture_double_tap.equals("NONE")) {
			
			final Class<?> LauncherClass = findClass(Common.LAUNCHER, lpparam.classLoader);			
			XposedBridge.hookAllMethods(LauncherClass, "onClick", new XC_MethodHook() {
				
				// http://androidxref.com/4.4.2_r1/xref/packages/apps/Launcher3/src/com/android/launcher3/LauncherSettings.java
				// https://github.com/CyanogenMod/android_packages_apps_Trebuchet/blob/cm-11.0/src/com/android/launcher3/LauncherSettings.java
				final int ITEM_TYPE_ALLAPPS = 5;
				final int ITEM_TYPE_FOLDER = 2;
				
				Runnable r = new Runnable() {
					
					@Override
					public void run() {
						
						if (DEBUG) log("Doubletap: " + currTouchTime + " " + lastTouchTime + " " + (currTouchTime == lastTouchTime) + " " + (currTouchTime - lastTouchTime));
						
						if (currTouchTime == lastTouchTime) {
							
							callMethod(Common.LAUNCHER_INSTANCE, "onClick", lastTouchView);
							
						} else if ((currTouchTime - lastTouchTime) < 400) {
							
							handleGesture(getGestureKey(Gestures.DOUBLE_TAP), PreferencesHelper.gesture_double_tap);							
						}
					}
				};
				
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					currTouchTime = System.currentTimeMillis();
					
					if (getObjectField(Common.WORKSPACE_INSTANCE, "mState").toString().equals("NORMAL")) {
						
						View view = (View) param.args[0];
						Object tag = view.getTag();
						if (tag == null) return;
						
						if (PreferencesHelper.gesture_double_tap_only_on_wallpaper) {
							
							if (!view.getTag().getClass().getName().contains("CellInfo")) {
								
								return;
							}
							
						} else {
						
							if (view.getTag().getClass().getName().contains("ShortcutInfo")) {
								
								int itemType = getIntField(tag, "itemType");
								if (itemType == ITEM_TYPE_ALLAPPS
									|| itemType == ITEM_TYPE_FOLDER) {
									return;
								}	
							} else if (view.getTag().getClass().getName().contains("FolderInfo")) {
								
								return;
							}
						}
						
						if (isScheduledOrRunning) {
							isScheduledOrRunning = false;
							
							if (DEBUG) log("Doubletap: isScheduledOrRunning");
							if (delayedTask.getDelay(TimeUnit.MILLISECONDS) > 0) {
								if (DEBUG) log("Doubletap: ignore tap");
								param.setResult(null);
							}
						} else {
							if (DEBUG)  log("!isScheduledOrRunning");
							
							if (executor.getQueue().size() == 0) {
								if (DEBUG) log("Doubletap: Schedule double tap action");
								
								lastTouchTime = currTouchTime;
								lastTouchView = view;
								
								delayedTask = executor.schedule(r, 400, TimeUnit.MILLISECONDS);
								isScheduledOrRunning = true;
								
								param.setResult(null);
							}						
						}
					}
				}
			});
		}
	}
}