//
//  AppDelegate.m
//  PushSDKForJavaScript
//
//  Created by gywang on 18/5/12.
//  Copyright © 2018年 mob.com. All rights reserved.
//

#import "AppDelegate.h"
#import "PushSDKJSBridge.h"

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {

//设置推送环境
#if DEBUG
    [[PushSDKJSBridge sharedBridge] setPushNotifyReleaseEnvironment:NO];
#else
    [[PushSDKJSBridge sharedBridge] setPushNotifyReleaseEnvironment:YES];
#endif
    
    return YES;
}

@end
