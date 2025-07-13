#import <Cordova/CDVPlugin.h>

@interface UniversalLinks : CDVPlugin

- (void)subscribe:(CDVInvokedUrlCommand*)command;
- (void)handleUserActivity:(NSUserActivity *)userActivity;

@end
