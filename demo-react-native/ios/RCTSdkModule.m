//
//  RCTSdkModule.m
//  demoReactNative
//
//  Created by Florian Otto on 20.08.24.
//

#import "RCTSdkModule.h"

#import <Epotheke/epotheke.h>
#import <OpenEcard/open-ecard.h>
#import <React/RCTLog.h>

@interface LogMsgHandler : NSObject <LogMessageHandler>
@property NSMutableString* log;
@end
@implementation LogMsgHandler
- (void)log:(NSString *)msg {
    @synchronized (self){
        //RCTLogInfo(@"bridgeLog: injected logger msg", msg);
        if(!self.log){
            self.log = [NSMutableString new];
        }

        [self.log appendString: msg];
    }
}

@end

@interface IOSNFCOptions : NSObject <NFCConfig>
@end
@implementation IOSNFCOptions
- (NSString *)getAquireNFCTagTimeoutMessage {
    return @"Could not connect to a card. Please try again.";
}

- (NSString *)getDefaultCardConnectedMessage {
    return @"Connected with the card.";
}

- (NSString *)getDefaultNFCCardRecognizedMessage {
    return @"Please wait. Card has been recognized.";
}

- (NSString *)getDefaultNFCErrorMessage {
    return @"An error occurred communicating with the card.";
}

- (NSString *)getNFCCompletionMessage {
    return @"Finished communicating with the card.";
}

- (NSString *)getProvideCardMessage {
    return @"Please hold card to your phone.";
}

- (NSString *)getTagLostErrorMessage {
    return @"An error occurred communicating with the card.";
}

- (NSString *)getDefaultCardInsertedMessage {
    return @"Please wait. A card has been inserted.";
}

@end

@interface SdkErroHandler : NSObject <EpothekeSdkErrorHandler>
@property RCTResponseSenderBlock onSdkErrorCB;
@end

@implementation SdkErroHandler
- (void)hdlCode:(nonnull NSString *)code error:(nonnull NSString *)error {
    RCTLogInfo(@"bridgeLog: error code:%@ msg: %@", code, error);
    self.onSdkErrorCB(@[ code, error ]);
}

@end

@interface CardLinkControllerCallback : NSObject <EpothekeCardLinkControllerCallback>
@property RCTResponseSenderBlock onStartedCB;
@property RCTResponseSenderBlock onAuthenticationCompletionCB;
@property EpothekePrescriptionProtocolImp *prescriptionProtocol;
@property NSString *wsSessionID;
@property NSString *lastIccsn;
@property NSString *lastPersonalData;
@end

@implementation CardLinkControllerCallback
- (void)onAuthenticationCompletionP0:(id<ActivationResult> _Nullable)p0
                   cardLinkProtocols:(nonnull NSSet<id<EpothekeCardLinkProtocol>> *)cardLinkProtocols {
    RCTLogInfo(@"bridgeLog: onAuthComp");

    for (NSObject *p in cardLinkProtocols) {
        if ([p conformsToProtocol:@protocol(EpothekePrescriptionProtocol)]) {
            self.prescriptionProtocol = p;
        }
    }
    if([p0 getResultCode] == kActivationResultCodeOK){
        if (self.onAuthenticationCompletionCB) {
                    self.wsSessionID = [p0 getResultParameter:@"CardLink::WS_SESSION_ID"];
                    self.lastIccsn= [p0 getResultParameter:@"CardLink::ICCSN"];
                    self.lastPersonalData= [p0 getResultParameter:@"CardLink::PERSONAL_DATA"];
                    self.onAuthenticationCompletionCB(@[ [NSNull null], [NSNull null] ] );
        }
    } else {
        if ([[p0 getErrorMessage] rangeOfString:@"==>"].location == NSNotFound){
            self.onAuthenticationCompletionCB(@[ @"INTERRUPTED", [p0 getErrorMessage] ] );
        } else {
            NSString *code = [[p0 getErrorMessage] componentsSeparatedByString:@" ==> "][0];
            NSString *msg = [[p0 getErrorMessage] componentsSeparatedByString:@" ==> "][1];
            if([code rangeOfString:@"invalidSlotHandle"].location != NSNotFound){
                self.onAuthenticationCompletionCB(@[ @"CARD_REMOVED", msg ? msg : @"no msg" ] );
            }else{
                self.onAuthenticationCompletionCB(@[ code, msg ] );
            }
        }
    }
}

- (void)onStarted {
    RCTLogInfo(@"bridgeLog: onStarted");
    if (self.onStartedCB) {
        self.onStartedCB(@[ @[ @"onStarted" ] ]);
    }
}
@end

@interface CardLinkInterActionImp : NSObject <CardLinkInteraction>

@property(nonatomic, copy, nullable) void (^userInputDispatch)(NSString *input);

@property RCTResponseSenderBlock requestCardInsertionCB;
@property RCTResponseSenderBlock onCardInteractionCompleteCB;
@property RCTResponseSenderBlock onCardInsertedCB;
@property RCTResponseSenderBlock onCardInsufficientCB;
@property RCTResponseSenderBlock onCardRecognizedCB;
@property RCTResponseSenderBlock onCardRemovedCB;
@property RCTResponseSenderBlock onCanRequestCB;
@property RCTResponseSenderBlock onCanRetryCB;
@property RCTResponseSenderBlock onPhoneNumberRequestCB;
@property RCTResponseSenderBlock onPhoneNumberRetryCB;
@property RCTResponseSenderBlock onSmsCodeRequestCB;
@property RCTResponseSenderBlock onSmsCodeRetryCB;
@end

@implementation CardLinkInterActionImp

- (void)onCardInteractionComplete {
    RCTLogInfo(@"bridgeLog: onCardInteractionComplete ");
    if (self.onCardInteractionCompleteCB)
        self.onCardInteractionCompleteCB(nil);
}

- (void)onCardRecognized {
    RCTLogInfo(@"bridgeLog: onCardRecognized ");
    if (self.onCardRecognizedCB)
        self.onCardRecognizedCB(nil);
}
- (void)onCardInserted {
    RCTLogInfo(@"bridgeLog: onCardInserted");
    if (self.onCardInsertedCB)
        self.onCardInsertedCB(nil);
}
- (void)onCardInsufficient {
    RCTLogInfo(@"bridgeLog: onCardInsufficient");
    if (self.onCardInsufficientCB)
        self.onCardInsufficientCB(nil);
}
- (void)onCardRemoved {
    RCTLogInfo(@"bridgeLog: onCardRemoved");
    if (self.onCardRemovedCB)
        self.onCardRemovedCB(nil);
}

- (void)requestCardInsertion {
    RCTLogInfo(@"bridgeLog: requestCardInsertion");
    if (self.requestCardInsertionCB)
        self.requestCardInsertionCB(nil);
}

- (void)requestCardInsertion:(NSObject<NFCOverlayMessageHandler> *)msgHandler {
    RCTLogInfo(@"bridgeLog: requestCardInsertion");
    if (self.requestCardInsertionCB)
        self.requestCardInsertionCB(nil);
}

- (void)onCanRequest:(NSObject<ConfirmPasswordOperation> *)enterCan {
    RCTLogInfo(@"bridgeLog: onCanRequest");
    if (enterCan && self.onCanRequestCB) {
        self.userInputDispatch = ^(NSString *input) {
          if ([enterCan conformsToProtocol:@protocol(ConfirmPasswordOperation)]) {
              [enterCan confirmPassword:input];
          }
        };
        self.onCanRequestCB(nil);
    }
}

- (void)onCanRetry:(NSObject<ConfirmPasswordOperation> *)enterCan 
		   withResultCode:(NSString*)resultCode
		   withErrorMessage:(NSString*)errorMessage {
    RCTLogInfo(@"bridgeLog: onCanRetry");
    if (enterCan && self.onCanRequestCB) {
        self.userInputDispatch = ^(NSString *input) {
          if ([enterCan conformsToProtocol:@protocol(ConfirmPasswordOperation)]) {
              [enterCan confirmPassword:input];
          }
        };
        if(!resultCode){
            resultCode = @"UNKNOWN_ERROR";
        }
        if(!errorMessage){
            errorMessage = @"No detailed message available.";
        }
        self.onCanRetryCB(@[resultCode, errorMessage]);
    }
}

- (void)onPhoneNumberRequest:(NSObject<ConfirmTextOperation> *)enterPhoneNumber {
    RCTLogInfo(@"bridgeLog: onPhoneNumberRequest");
    if (enterPhoneNumber && self.onPhoneNumberRequestCB) {
        self.userInputDispatch = ^(NSString *input) {
          if ([enterPhoneNumber conformsToProtocol:@protocol(ConfirmTextOperation)]) {
              [enterPhoneNumber confirmText:input];
          }
        };
        self.onPhoneNumberRequestCB(nil);
    }
}
- (void)onPhoneNumberRetry:(NSObject<ConfirmTextOperation> *)enterPhoneNumber 
		   withResultCode:(NSString*)resultCode
		   withErrorMessage:(NSString*)errorMessage {
    RCTLogInfo(@"bridgeLog: onPhoneNumberRetry");
    if (enterPhoneNumber && self.onPhoneNumberRequestCB) {
        self.userInputDispatch = ^(NSString *input) {
          if ([enterPhoneNumber conformsToProtocol:@protocol(ConfirmTextOperation)]) {
              [enterPhoneNumber confirmText:input];
          }
        };
        if(!resultCode){
            resultCode = @"UNKNOWN_ERROR";
        }
        if(!errorMessage){
            errorMessage = @"No detailed message available.";
        }
        self.onPhoneNumberRetryCB(@[resultCode,errorMessage]);
    }
}

- (void)onSmsCodeRequest:(NSObject<ConfirmPasswordOperation> *)smsCode {
    RCTLogInfo(@"bridgeLog: onSmsCodeRequest");
    if (smsCode && self.onSmsCodeRequestCB) {
        self.userInputDispatch = ^(NSString *input) {
          if ([smsCode conformsToProtocol:@protocol(ConfirmPasswordOperation)]) {
              [smsCode confirmPassword:input];
          }
        };
        self.onSmsCodeRequestCB(nil);
    }
}
- (void)onSmsCodeRetry:(NSObject<ConfirmPasswordOperation> *)smsCode 
		   withResultCode:(NSString*)resultCode
		   withErrorMessage:(NSString*)errorMessage {
    RCTLogInfo(@"bridgeLog: onSmsCodeRetry");
    if (smsCode && self.onSmsCodeRequestCB) {
        self.userInputDispatch = ^(NSString *input) {
          if ([smsCode conformsToProtocol:@protocol(ConfirmPasswordOperation)]) {
              [smsCode confirmPassword:input];
          }
        };
        if(!resultCode){
            resultCode = @"UNKNOWN_ERROR";
        }
        if(!errorMessage){
            errorMessage = @"No detailed message available.";
        }
        self.onSmsCodeRetryCB(@[resultCode, errorMessage]);
    }
}
@end

@implementation RCTSdkModule
// To export a module named RCTCalendarModule
RCT_EXPORT_MODULE();

SdkErroHandler *errHandler;
CardLinkInterActionImp *clInteraction;
CardLinkControllerCallback *clCtrlCB;

RCT_EXPORT_METHOD(setUserInput : (NSString *)val) {
    RCTLogInfo(@"bridgeLog: got UserInput %@", val);
    if (clInteraction && clInteraction.userInputDispatch) {
        clInteraction.userInputDispatch(val);
    }
}

RCT_EXPORT_METHOD(set_controllerCallbackCB_onStarted : (RCTResponseSenderBlock)cb) {
    if (!clCtrlCB) {
        clCtrlCB = [CardLinkControllerCallback new];
    }
    clCtrlCB.onStartedCB = cb;
}
RCT_EXPORT_METHOD(set_controllerCallbackCB_onAuthenticationCompletion : (RCTResponseSenderBlock)cb) {
    if (!clCtrlCB) {
        clCtrlCB = [CardLinkControllerCallback new];
    }
    clCtrlCB.onAuthenticationCompletionCB = cb;
}

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_requestCardInsertion : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.requestCardInsertionCB = cb;
}
RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onCardInteractionComplete : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onCardInteractionCompleteCB = cb;
}

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onCardRecognized : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onCardRecognizedCB = cb;
}

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onCardInserted : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onCardInsertedCB = cb;
}

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onCardInsufficient : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onCardInsufficientCB = cb;
}

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onCardRemoved : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onCardRemovedCB = cb;
}

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onCanRequest : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onCanRequestCB = cb;
}
RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onCanRetry : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onCanRetryCB = cb;
}

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onPhoneNumberRequest : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onPhoneNumberRequestCB = cb;
}
RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onPhoneNumberRetry : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onPhoneNumberRetryCB = cb;
}

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onSmsCodeRequest : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onSmsCodeRequestCB = cb;
}
RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onSmsCodeRetry : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onSmsCodeRetryCB = cb;
}

RCTResponseSenderBlock onSdkError;
RCT_EXPORT_METHOD(set_sdkErrorCB : (RCTResponseSenderBlock)cb) {
    if (!errHandler) {
        errHandler = [SdkErroHandler new];
    }
    errHandler.onSdkErrorCB = cb;
}

RCT_EXPORT_METHOD(getWsSessionId : (RCTPromiseResolveBlock)resolve  : (RCTPromiseRejectBlock)reject) {
    
    dispatch_sync(dispatch_get_main_queue(), ^{
        if (clCtrlCB && clCtrlCB.wsSessionID) {
            resolve(clCtrlCB.wsSessionID);
        } else {
            resolve(nil);
        }
    });
}
RCT_EXPORT_METHOD(getLastICCSN: (RCTPromiseResolveBlock)resolve  : (RCTPromiseRejectBlock)reject) {
    
    dispatch_sync(dispatch_get_main_queue(), ^{
        if (clCtrlCB && clCtrlCB.lastIccsn) {
            resolve(clCtrlCB.lastIccsn);
        } else {
            resolve(nil);
        }
    });
}
RCT_EXPORT_METHOD(getLastPersonalData: (RCTPromiseResolveBlock)resolve  : (RCTPromiseRejectBlock)reject) {

    dispatch_sync(dispatch_get_main_queue(), ^{
        if (clCtrlCB && clCtrlCB.lastPersonalData) {
            resolve(clCtrlCB.lastPersonalData);
        } else {
            resolve(nil);
        }
    });
}
RCT_EXPORT_METHOD(getPrescriptions : (NSArray*)filter: (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {

    dispatch_sync(dispatch_get_main_queue(),
 ^{
      if (clCtrlCB && clCtrlCB.prescriptionProtocol) {
          if ([clCtrlCB.prescriptionProtocol conformsToProtocol:@protocol(EpothekePrescriptionProtocol)]) {
              [clCtrlCB.prescriptionProtocol
               requestPrescriptionsIccsns:filter
               messageId:@""
                        completionHandler:^(EpothekeAvailablePrescriptionLists *res, NSError *err) {
                          if (err) {
                              reject(@"", @"", err);
                          } else {
                            resolve(
                                        [[EpothekeJsonHelper alloc] availablePrescriptionListsJsonToStringPrescriptionLists:res]
                                    );
                          }}];
          }
      }
    });
}

RCT_EXPORT_METHOD(selectPrescriptions: (NSString *)selection
 resolver: (RCTPromiseResolveBlock)resolve
 rejecter: (RCTPromiseRejectBlock)reject) {

    dispatch_sync(dispatch_get_main_queue(), ^{
      if (clCtrlCB && clCtrlCB.prescriptionProtocol) {
          if ([clCtrlCB.prescriptionProtocol conformsToProtocol:@protocol(EpothekePrescriptionProtocol)]) {

            [clCtrlCB.prescriptionProtocol
             selectPrescriptionsSelection: [[EpothekeJsonHelper alloc]getSelectedPrescriptionListFromJsonString:selection]
             completionHandler:^(EpothekeSelectedPrescriptionListResponse * confirm, NSError * err){
                   resolve(
                           [[EpothekeJsonHelper alloc] selectedPrescriptionListResponseJsonToStringSelection:confirm]
                   );
                }
              ];
          }
      }}
    );
}


EpothekeSdkCore *sdk;
LogMsgHandler *msgHandler;
RCT_EXPORT_METHOD(getLog: (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
        dispatch_sync(dispatch_get_main_queue(), ^{
            if (msgHandler) {
                resolve(msgHandler.log);
        } else {
            resolve(@"no log");
        }
    });
}


RCT_EXPORT_METHOD(startCardLink : (NSString *)cardLinkUrl tenantToken: (NSString *) tenantToken) {
    RCTLogInfo(@"bridgeLog: onStarted: %@", cardLinkUrl);

    IOSNFCOptions *nfcOpts = [IOSNFCOptions new];
    
    if(!sdk){
        sdk = [[EpothekeSdkCore alloc] initWithCardLinkControllerCallback:clCtrlCB
                                            cardLinkInteractionProtocol:clInteraction
                                                        sdkErrorHandler:errHandler
                                                                nfcOpts:nfcOpts];
    }

    msgHandler = [LogMsgHandler new];
    [sdk setLogMessageHandlerHandler:msgHandler];
    [sdk setDebugLogLevel];
    [sdk activateWaitForSlot:false cardLinkUrl:cardLinkUrl tenantToken:tenantToken];
}

RCT_EXPORT_METHOD(activationActive: (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        if(sdk){
            if([sdk activationsActive]) {
                resolve(@TRUE);
                return;
            }
        }
        resolve(@FALSE);
    });
}
RCT_EXPORT_METHOD(destroyCardlinkResources) {
    if(sdk){
        [sdk destroyOecContext];
        sdk = nil;
    }
}

RCT_EXPORT_METHOD(abortCardLink) {
    if(sdk){
        [sdk cancelOngoingActivation];
    }
}

@end
