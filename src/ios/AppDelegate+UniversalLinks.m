#import "AppDelegate+UniversalLinks.h"
#import "UniversalLinks.h" // The header for our main plugin class

@implementation AppDelegate (UniversalLinks)

/**
 * This method is called by the OS when the app is opened by a universal link.
 * We then pass the event to our plugin's handler.
 */
- (BOOL)application:(UIApplication *)application continueUserActivity:(NSUserActivity *)userActivity restorationHandler:(void (^)(NSArray * _Nullable))restorationHandler
{
    // Check if the activity is for a web browsing URL
    if ([userActivity.activityType isEqualToString:NSUserActivityTypeBrowsingWeb]) {
        // Get an instance of our UniversalLinks plugin
        UniversalLinks* plugin = [self.viewController getCommandInstance:@"UniversalLinks"];

        // If the plugin exists, call its handler method
        if (plugin != nil) {
            [plugin handleUserActivity:userActivity];
        }
    }

    return YES;
}

@end
