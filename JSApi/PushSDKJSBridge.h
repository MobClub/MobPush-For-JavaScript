//
//  PushSDKJSBridge.h
//  PushSDKForJavaScript
//
//  Created by gywang on 18/5/12.
//  Copyright © 2018年 mob.com. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface PushSDKJSBridge : NSObject
/**
 *    @brief    获取共享桥接器实例
 *
 *    @return    JS桥接器
 */
+ (PushSDKJSBridge *)sharedBridge;
/**
 *	@brief	捕获WebView中请求，将此方法放入webView:shouldStartLoadWithRequest:navigationType:委托方法中
 *
 *	@param 	request 	请求对象
 *  @param  webView     Web视图对象
 *
 *	@return	YES 表示为PushSDK接口请求，请求被捕获。NO 表示非PushSDK接口请求，不捕获请求
 */
- (BOOL)captureRequest:(NSURLRequest *)request webView:(UIWebView *)webView;


/**
 程序启动时设置推送环境

 @param isRelease 是否生产版本
 */
- (void)setPushNotifyReleaseEnvironment:(BOOL)isRelease;

@end
