#import "AppDelegate.h"

/**
 * This is a category on the main AppDelegate. It allows our plugin to receive
 * the continueUserActivity event from the OS without requiring the user to
 * manually edit their AppDelegate.m file.
 */
@interface AppDelegate (UniversalLinks)

- (BOOL)application:(UIApplication *)application continueUserActivity:(NSUserActivity *)userActivity restorationHandler:(void (^)(NSArray * _Nullable))restorationHandler;

@end
