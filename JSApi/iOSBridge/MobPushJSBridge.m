//
//  MobPushJSBridge.m
//  MobPushForJavaScript
//
//  Created by gywang on 18/5/12.
//  Copyright © 2018年 mob.com. All rights reserved.
//

#import "MobPushJSBridge.h"
#import <MOBFoundation/MOBFoundation.h>
#import <MobPush/MobPush.h>
#import <MobPush/MobPush+Test.h>

static NSString *const initMobPushSDK = @"initMobPushSDK";
static NSString *const sendCustomMsg = @"sendCustomMsg";
static NSString *const sendAPNsMsg = @"sendAPNsMsg";
static NSString *const sendLocalNotify = @"sendLocalNotify";
static NSString *const getRegistrationID = @"getRegistrationID";
static NSString *const setAlias = @"setAlias";
static NSString *const getAlias = @"getAlias";
static NSString *const deleteAlias = @"deleteAlias";
static NSString *const addTags = @"addTags";
static NSString *const getTags = @"getTags";
static NSString *const deleteTags = @"deleteTags";
static NSString *const cleanAllTags = @"cleanAllTags";
static MobPushJSBridge *_instance = nil;

//#ifdef DEBUG
//
//@interface UIWebView (JavaScriptAlert)
//
//- (void)webView:(UIWebView *)sender runJavaScriptAlertPanelWithMessage:(NSString *)message initiatedByFrame:(id)frame;
//
//@end
//
//@implementation UIWebView (JavaScriptAlert)
//
//- (void)webView:(UIWebView *)sender runJavaScriptAlertPanelWithMessage:(NSString *)message initiatedByFrame:(id)frame
//{
//    NSLog(@"%@", message);
//}
//
//@end
//
//#endif

@interface MobPushJSBridge ()
{
    UIWebView *_webView;
    NSString *_seqId;
    NSString *_callback;
}

@end

@implementation MobPushJSBridge
+ (MobPushJSBridge *)sharedBridge
{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        if (_instance == nil)
        {
            _instance = [[MobPushJSBridge alloc] init];
        }
    });
    return _instance;
}

- (void)setPushNotifyReleaseEnvironment:(BOOL)isRelease
{
    //设置推送环境
    [MobPush setAPNsForProduction:isRelease];
    //MobPush推送设置（获得角标、声音、弹框提醒权限）
    MPushNotificationConfiguration *configuration = [[MPushNotificationConfiguration alloc] init];
    configuration.types = MPushAuthorizationOptionsBadge | MPushAuthorizationOptionsSound | MPushAuthorizationOptionsAlert;
    [MobPush setupNotification:configuration];
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didReceiveMessage:) name:MobPushDidReceiveMessageNotification object:nil];
}

//接收推送消息回调
- (void)didReceiveMessage:(NSNotification *)notification
{
    MPushMessage *message = notification.object;
    NSMutableDictionary *resultDict = [NSMutableDictionary dictionary];
    if (_seqId) {
        [resultDict setObject:_seqId forKey:@"seqId"];
    }
    
    
    switch (message.messageType)
    {
        case MPushMessageTypeCustom:
        {// 自定义消息
            if (message.extraInfomation)
            {
                [resultDict setObject:message.extraInfomation forKey:@"extra"];
            }
            if (message.content)
            {
                [resultDict setObject:message.content forKey:@"content"];
            }
            if (message.messageID)
            {
                [resultDict setObject:message.messageID forKey:@"messageId"];
            }
            if (message.currentServerTimestamp)
            {
                [resultDict setObject:@(message.currentServerTimestamp) forKey:@"timeStamp"];
            }
            
            [resultDict setObject:sendCustomMsg forKey:@"method"];
            
            [resultDict setObject:@"(function (reqID, content ,messageId) {                alert(content);                alert(\"messageId = \" + messageId);                                               })" forKey:@"callback"];
            
            [self resultWithData:resultDict webView:_webView];
        }
            break;
        case MPushMessageTypeAPNs:
        {// APNs 回调
            /*
             {
             1 = 2;
             aps =     {
             alert =         {
             body = 1;
             subtitle = 1;
             title = 1;
             };
             "content-available" = 1;
             "mutable-content" = 1;
             };
             mobpushMessageId = 159346875878223872;
             }
             */
            if (message.msgInfo)
            {
                NSDictionary *aps = message.msgInfo[@"aps"];
                if ([aps isKindOfClass:[NSDictionary class]])
                {
                    NSDictionary *alert = aps[@"alert"];
                    if ([alert isKindOfClass:[NSDictionary class]])
                    {
                        NSString *body = alert[@"body"];
                        if (body)
                        {
                            [resultDict setObject:body forKey:@"content"];
                        }
                        
                        NSString *subtitle = alert[@"subtitle"];
                        if (subtitle)
                        {
                            [resultDict setObject:subtitle forKey:@"subtitle"];
                        }
                        
                        NSString *title = alert[@"title"];
                        if (title)
                        {
                            [resultDict setObject:title forKey:@"title"];
                        }
                    }
                    
                    NSString *sound = aps[@"sound"];
                    if (sound)
                    {
                        [resultDict setObject:sound forKey:@"sound"];
                    }
                    
                    NSInteger badge = [aps[@"badge"] integerValue];
                    if (badge)
                    {
                        [resultDict setObject:@(badge) forKey:@"badge"];
                    }
                    
                }
                
                NSString *mobpushMessageId = message.msgInfo[@"mobpushMessageId"];
                if (mobpushMessageId)
                {
                    [resultDict setObject:mobpushMessageId forKey:@"mobpushMessageId"];
                }
                
                NSMutableDictionary *extra = [NSMutableDictionary dictionary];
                [message.msgInfo enumerateKeysAndObjectsUsingBlock:^(id  _Nonnull key, id  _Nonnull obj, BOOL * _Nonnull stop) {
                    if (![key isEqualToString:@"aps"] && ![key isEqualToString:@"mobpushMessageId"])
                    {
                        [extra setObject:obj forKey:key];
                    }
                    
                }];
                if (extra)
                {
                    [resultDict setObject:extra forKey:@"extra"];
                }
                
                [resultDict setObject:sendAPNsMsg forKey:@"method"];
                
                [resultDict setObject:@"(function (reqID, body , mobpushMessageId) {                alert(body);                alert(\"mobpushMessageId = \" + mobpushMessageId);                                               })" forKey:@"callback"];
                
                [self resultWithData:resultDict webView:_webView];
            }
            
        }
            break;
        case MPushMessageTypeLocal:
        { // 本地通知回调
            NSString *body = message.notification.body;
            NSString *title = message.notification.title;
            NSString *subtitle = message.notification.subTitle;
            NSInteger badge = message.notification.badge;
            NSString *sound = message.notification.sound;
            if (body)
            {
                [resultDict setObject:body forKey:@"content"];
            }
            if (title)
            {
                [resultDict setObject:title forKey:@"title"];
            }
            if (subtitle)
            {
                [resultDict setObject:subtitle forKey:@"subtitle"];
            }
            if (badge)
            {
                [resultDict setObject:@(badge) forKey:@"badge"];
            }
            if (sound)
            {
                [resultDict setObject:sound forKey:@"sound"];
            }
            
            [resultDict setObject:sendLocalNotify forKey:@"method"];
            
            if (_callback) {
                [resultDict setObject:_callback forKey:@"callback"];
            }
            
            [self resultWithData:resultDict webView:_webView];
        }
            break;
        default:
            
            break;
    }
    
    
}

- (BOOL)captureRequest:(NSURLRequest *)request webView:(UIWebView *)webView
{
    _webView = webView;
    if ([request.URL.scheme isEqualToString:@"mobpush"])
    {
        if ([request.URL.host isEqualToString:@"init"])
        {
            //初始化JS
            [webView stringByEvaluatingJavaScriptFromString:@"window.$mobpush.initMobPushJS(2)"];
        }
        else if ([request.URL.host isEqualToString:@"call"])
        {
            //调用接口
            NSDictionary *params = [MOBFString parseURLParametersString:request.URL.query];
            NSString *methodName = [params objectForKey:@"methodName"];
            NSString *seqId = [params objectForKey:@"seqId"];
            
            NSDictionary *paramsDict = nil;
            NSString *paramsStr = [webView stringByEvaluatingJavaScriptFromString:[NSString stringWithFormat:@"$mobpush.getParams(%@)",seqId]];
            
            if (paramsStr)
            {
                paramsDict = [MOBFJson objectFromJSONString:paramsStr];
            }
            
            if ([methodName isEqualToString:initMobPushSDK]) {
                //初始化MobPushSDK
                //                [self initMobPushSDKWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:sendCustomMsg] || [methodName isEqualToString:sendAPNsMsg]){
                //发送消息(应用内，APNs)
                [self sendMessageWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:sendLocalNotify]){
                [self sendLocalNotifyWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:getRegistrationID]){
                [self getRegistrationIDWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:setAlias]){
                [self setAliasWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:getAlias]){
                [self getAliasWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:deleteAlias]){
                [self deleteAliasWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:addTags]){
                [self addTagsWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:getTags]){
                [self getTagsWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:deleteTags]){
                [self deleteTagsWithSeqId:seqId params:paramsDict webView:webView];
            }else if ([methodName isEqualToString:cleanAllTags]){
                [self cleanAllTagsWithSeqId:seqId params:paramsDict webView:webView];
            }
        }
        
        return YES;
    }
    
    return NO;
}


#pragma mark - Private -
/**
 *    @brief    返回数据给js
 *
 *    @param     data     回调数据
 */
- (void)resultWithData:(NSDictionary *)data webView:(UIWebView *)webView
{
    dispatch_async(dispatch_get_main_queue(), ^{
        NSString * methodNameStr = [NSString stringWithFormat:@"$mobpush.callback(%@)", [MOBFJson jsonStringFromObject:data]];
        NSString *jsMyAlert =[NSString stringWithFormat:@"setTimeout(function(){%@;},1)",methodNameStr];
        [webView stringByEvaluatingJavaScriptFromString:jsMyAlert];
    });
}

//发送消息(应用内，APNs)
- (void)sendMessageWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSDictionary * dict = nil;
    if ([params[@"msgParams"] isKindOfClass:[NSDictionary class]]) {
        dict = params[@"msgParams"];
    }
    NSInteger  msgType = [dict[@"msgType"] integerValue];
    NSString * content = nil;
    if ([dict[@"content"] isKindOfClass:[NSString class]]) {
        content = dict[@"content"];
    }
    NSInteger timedSpace = [dict[@"timedSpace"] integerValue];
    BOOL isProductionEnvironment = [dict[@"isProductionEnvironment"] boolValue];
    _seqId = seqId;
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    
    [MobPush sendMessageWithMessageType:msgType
                                content:content
                                  space:@(timedSpace)
                isProductionEnvironment:isProductionEnvironment
                                 extras:nil
                             linkScheme:@""
                               linkData:@""
                                 result:^(NSError *error) {
                                     
                                     if (error)
                                     {
                                         NSLog(@"发送失败");
                                     }
                                     else
                                     {
                                         NSLog(@"发送成功");
                                     }
                                     
                                 }];
}


//发送本地通知
- (void)sendLocalNotifyWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSDictionary * dict = nil;
    if ([params[@"msgParams"] isKindOfClass:[NSDictionary class]]) {
        dict = params[@"msgParams"];
    }
    NSInteger  msgType = [dict[@"msgType"] integerValue];
    NSString * content = nil;
    if ([dict[@"content"] isKindOfClass:[NSString class]]) {
        content = dict[@"content"];
    }
    NSString * title = nil;
    if ([dict[@"title"] isKindOfClass:[NSString class]]) {
        title = dict[@"title"];
    }
    NSString * subTitle = nil;
    if ([dict[@"subTitle"] isKindOfClass:[NSString class]]) {
        subTitle = dict[@"subTitle"];
    }
    NSString * sound = nil;
    if ([dict[@"sound"] isKindOfClass:[NSString class]]) {
        sound = dict[@"sound"];
    }
    NSInteger badge = [dict[@"badge"] integerValue];
    _seqId = seqId;
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    
    MPushMessage *message = [[MPushMessage alloc] init];
    message.messageType = msgType;
    MPushNotification *noti = [[MPushNotification alloc] init];
    noti.body = content;
    noti.title = title;
    noti.subTitle = subTitle;
    noti.sound = sound;
    noti.badge = badge;
    message.notification = noti;
    message.isInstantMessage = YES;
    [MobPush addLocalNotification:message];
    
}

//获取注册id（可与用户id绑定，实现向指定用户推送消息）
- (void)getRegistrationIDWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSMutableDictionary * dictResult = [NSMutableDictionary dictionary];
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    dictResult[@"callback"] = _callback;
    dictResult[@"method"] = getRegistrationID;
    dictResult[@"seqId"] = seqId;
    [MobPush getRegistrationID:^(NSString *registrationID, NSError *error) {
        if (!error) {
            if (registrationID) {
                dictResult[@"registrationID"] = registrationID;
            }
        }else{
            dictResult[@"errorCode"] = @(error.code);
            dictResult[@"errorMsg"] = error.userInfo;
        }
        
        [self resultWithData:dictResult webView:webView];
    }];
}

//设置别名
- (void)setAliasWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSMutableDictionary * dictResult = [NSMutableDictionary dictionary];
    NSDictionary * dict = nil;
    if ([params[@"msgParams"] isKindOfClass:[NSDictionary class]]) {
        dict = params[@"msgParams"];
    }
    NSString * alias = @"";
    if ([dict[@"alias"] isKindOfClass:[NSString class]]) {
        alias = dict[@"alias"];
    }
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    dictResult[@"callback"] = _callback;
    dictResult[@"method"] = setAlias;
    dictResult[@"seqId"] = seqId;
    [MobPush setAlias:alias result:^(NSError *error) {
        if (error) {
            dictResult[@"errorCode"] = @(error.code);
            dictResult[@"errorMsg"] = error.userInfo;
        }
        
        [self resultWithData:dictResult webView:webView];
    }];
}

//获取别名
- (void)getAliasWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSMutableDictionary * dictResult = [NSMutableDictionary dictionary];
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    dictResult[@"callback"] = _callback;
    dictResult[@"method"] = getAlias;
    dictResult[@"seqId"] = seqId;
    [MobPush getAliasWithResult:^(NSString *alias, NSError *error) {
        if (!error) {
            if (alias) {
                dictResult[@"alias"] = alias;
            }
        }else{
            dictResult[@"errorCode"] = @(error.code);
            dictResult[@"errorMsg"] = error.userInfo;
        }
        
        [self resultWithData:dictResult webView:webView];
    }];
    
}

//删除别名
- (void)deleteAliasWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSMutableDictionary * dictResult = [NSMutableDictionary dictionary];
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    dictResult[@"callback"] = _callback;
    dictResult[@"method"] = deleteAlias;
    dictResult[@"seqId"] = seqId;
    [MobPush deleteAlias:^(NSError *error) {
        if (error) {
            dictResult[@"errorCode"] = @(error.code);
            dictResult[@"errorMsg"] = error.userInfo;
        }
        
        [self resultWithData:dictResult webView:webView];
    }];
    
}

//添加标签
- (void)addTagsWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSMutableDictionary * dictResult = [NSMutableDictionary dictionary];
    NSDictionary * dict = nil;
    if ([params[@"msgParams"] isKindOfClass:[NSDictionary class]]) {
        dict = params[@"msgParams"];
    }
    NSArray * tags = [NSArray array];
    if ([dict[@"tags"] isKindOfClass:[NSArray class]]) {
        tags = dict[@"tags"];
    }
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    dictResult[@"callback"] = _callback;
    dictResult[@"method"] = addTags;
    dictResult[@"seqId"] = seqId;
    
    [MobPush addTags:tags result:^(NSError *error) {
        if (error) {
            dictResult[@"errorCode"] = @(error.code);
            dictResult[@"errorMsg"] = error.userInfo;
        }
        
        [self resultWithData:dictResult webView:webView];
    }];
    
}

//获取所有标签
- (void)getTagsWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSMutableDictionary * dictResult = [NSMutableDictionary dictionary];
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    dictResult[@"callback"] = _callback;
    dictResult[@"method"] = getTags;
    dictResult[@"seqId"] = seqId;
    
    [MobPush getTagsWithResult:^(NSArray *tags, NSError *error) {
        if (!error) {
            if (tags.count > 0) {
                dictResult[@"tags"] = tags;
            }
        }else{
            dictResult[@"errorCode"] = @(error.code);
            dictResult[@"errorMsg"] = error.userInfo;
        }
        
        [self resultWithData:dictResult webView:webView];
    }];
    
}

//删除标签
- (void)deleteTagsWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSMutableDictionary * dictResult = [NSMutableDictionary dictionary];
    NSDictionary * dict = nil;
    if ([params[@"msgParams"] isKindOfClass:[NSDictionary class]]) {
        dict = params[@"msgParams"];
    }
    NSArray * tags = [NSArray array];
    if ([dict[@"tags"] isKindOfClass:[NSArray class]]) {
        tags = dict[@"tags"];
    }
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    dictResult[@"callback"] = _callback;
    dictResult[@"method"] = deleteTags;
    dictResult[@"seqId"] = seqId;
    
    [MobPush deleteTags:tags result:^(NSError *error) {
        if (error) {
            dictResult[@"errorCode"] = @(error.code);
            dictResult[@"errorMsg"] = error.userInfo;
        }
        
        [self resultWithData:dictResult webView:webView];
    }];
    
}

//清空所有标签
- (void)cleanAllTagsWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
{
    NSMutableDictionary * dictResult = [NSMutableDictionary dictionary];
    if ([params[@"callback"] isKindOfClass:[NSString class]])
    {
        _callback = params[@"callback"];
    }
    dictResult[@"callback"] = _callback;
    dictResult[@"method"] = cleanAllTags;
    dictResult[@"seqId"] = seqId;
    
    [MobPush cleanAllTags:^(NSError *error) {
        if (error) {
            dictResult[@"errorCode"] = @(error.code);
            dictResult[@"errorMsg"] = error.userInfo;
        }
        
        [self resultWithData:dictResult webView:webView];
    }];
    
}

////初始化MobPushSDK
//- (void)initMobPushSDKWithSeqId:(NSString *)seqId params:(NSDictionary *)params webView:(UIWebView *)webView
//{
//    long pushEnvironment = 0;
//    if ([params[@"pushConfig"] isKindOfClass:[NSDictionary class]])
//    {
//        pushEnvironment = [params[@"pushConfig"] longValue];
//    }
//    //设置推送环境
//    [MobPush setAPNsForProduction:pushEnvironment];
//    //MobPush推送设置（获得角标、声音、弹框提醒权限）
//    MPushNotificationConfiguration *configuration = [[MPushNotificationConfiguration alloc] init];
//    configuration.types = MPushAuthorizationOptionsBadge | MPushAuthorizationOptionsSound | MPushAuthorizationOptionsAlert;
//    [MobPush setupNotification:configuration];
//
//    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didReceiveMessage:) name:MobPushDidReceiveMessageNotification object:nil];
//}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self name:MobPushDidReceiveMessageNotification object:nil];
}


@end
