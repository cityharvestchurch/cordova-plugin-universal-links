#import "UniversalLinks.h"
#import <Cordova/CDV.h>

@implementation UniversalLinks

- (void)pluginInitialize {
    // You can place any initialization code here.
}

- (void)subscribe:(CDVInvokedUrlCommand*)command {
    self.commandDelegate.keepCallback = YES;
    // Store the callback ID to be used later
    // In a real-world scenario, you would manage multiple subscribers.
    // For this example, we'll just store one.
    self.commandDelegate.callbackId = command.callbackId;
}

- (void)handleUserActivity:(NSUserActivity *)userActivity {
    if ([userActivity.activityType isEqualToString:NSUserActivityTypeBrowsingWeb]) {
        NSString *urlString = userActivity.webpageURL.absoluteString;
        if (urlString) {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:urlString];
            [pluginResult setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self.commandDelegate.callbackId];
        }
    }
}

@end
