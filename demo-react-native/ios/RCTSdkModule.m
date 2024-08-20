//
//  RCTSdkModule.m
//  demoReactNative
//
//  Created by Florian Otto on 20.08.24.
//

#import "RCTSdkModule.h"

#import <Epotheke/Epotheke.h>
#import <OpenEcard/open-ecard.h>
#import <React/RCTLog.h>

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
    return @"Please wait. A card has been detected";
}

- (NSString *)getDefaultNFCErrorMessage {
    return @"An error occurred communicating with the card.";
}

- (NSString *)getNFCCompletionMessage {
    return @"Finished communicating with the card";
}

- (NSString *)getProvideCardMessage {
    return @"Please hold card to your phone";
}

- (NSString *)getTagLostErrorMessage {
    return @"An error occurred communicating with the card.";
}
@end

@interface SdkErroHandler : NSObject <EpothekeSdkErrorHandler>
@property RCTResponseSenderBlock onSdkErrorCB;
@end

@implementation SdkErroHandler
- (void)hdlError:(NSObject<ServiceErrorResponse> *_Nullable)error {

    if ([error conformsToProtocol:@protocol(ServiceErrorResponse)]) {
        RCTLogInfo(@"error msg: %@", [error getErrorMessage]);
        self.onSdkErrorCB(@[ @[ [error getErrorMessage] ] ]);
    }
}
@end

@interface CardLinkControllerCallback : NSObject <EpothekeCardLinkControllerCallback>
@property RCTResponseSenderBlock onStartedCB;
@property RCTResponseSenderBlock onAuthenticationCompletionCB;
@property EpothekePrescriptionProtocolImp *prescriptionProtocol;
@end

@implementation CardLinkControllerCallback
- (void)onAuthenticationCompletionP0:(id<ActivationResult> _Nullable)p0
                   cardLinkProtocols:(nonnull NSSet<id<EpothekeCardLinkProtocol>> *)cardLinkProtocols {
    RCTLogInfo(@"onAuthComp");
    if (self.onAuthenticationCompletionCB) {
        for (NSObject *p in cardLinkProtocols) {
            if ([p conformsToProtocol:@protocol(EpothekePrescriptionProtocol)]) {
                // found prescriptionProto
                self.prescriptionProtocol = p;
                self.onAuthenticationCompletionCB(@[ @[ [NSNull null], @"PrescriptonProtocol" ] ]);
                break;
            }
        }
    }
}

- (void)onStarted {
    RCTLogInfo(@"onStarted");
    if (self.onStartedCB) {
        self.onStartedCB(@[ @[ @"onStarted" ] ]);
    }
}
@end

@interface CardLinkInterActionImp : NSObject <CardLinkInteraction>

@property(nonatomic, copy, nullable) void (^userInputDispatch)(NSString *input);

@property RCTResponseSenderBlock requestCardInsertionCB;
@property RCTResponseSenderBlock onCardInteractionCompleteCB;
@property RCTResponseSenderBlock onCardRecognizedCB;
@property RCTResponseSenderBlock onCardRemovedCB;
@property RCTResponseSenderBlock onCanRequestCB;
@property RCTResponseSenderBlock onPhoneNumberRequestCB;
@property RCTResponseSenderBlock onSmsCodeRequestCB;
@end

@implementation CardLinkInterActionImp

- (void)onCardInteractionComplete {
    RCTLogInfo(@"onCardInteractionComplete ");
    if (self.onCardInteractionCompleteCB)
        self.onCardInteractionCompleteCB(nil);
}

- (void)onCardRecognized {
    RCTLogInfo(@"onCardRecognized ");
    if (self.onCardRecognizedCB)
        self.onCardRecognizedCB(nil);
}

- (void)onCardRemoved {
    RCTLogInfo(@"onCardRemoved");
    if (self.onCardRemovedCB)
        self.onCardRemovedCB(nil);
}

- (void)requestCardInsertion {
    RCTLogInfo(@"requestCardInsertion");
    if (self.requestCardInsertionCB)
        self.requestCardInsertionCB(nil);
}

- (void)requestCardInsertion:(NSObject<NFCOverlayMessageHandler> *)msgHandler {
    RCTLogInfo(@"requestCardInsertion");
    if (self.requestCardInsertionCB)
        self.requestCardInsertionCB(nil);
}

- (void)onCanRequest:(NSObject<ConfirmPasswordOperation> *)enterCan {
    RCTLogInfo(@"onCanRequest");
    if (enterCan && self.onCanRequestCB) {
        self.userInputDispatch = ^(NSString *input) {
          if ([enterCan conformsToProtocol:@protocol(ConfirmPasswordOperation)]) {
              [enterCan confirmPassword:input];
          }
        };
        self.onCanRequestCB(nil);
    }
}

- (void)onPhoneNumberRequest:(NSObject<ConfirmTextOperation> *)enterPhoneNumber {
    RCTLogInfo(@"onPhoneNumberRequest");
    if (enterPhoneNumber && self.onPhoneNumberRequestCB) {
        self.userInputDispatch = ^(NSString *input) {
          if ([enterPhoneNumber conformsToProtocol:@protocol(ConfirmTextOperation)]) {
              [enterPhoneNumber confirmText:input];
          }
        };
        self.onPhoneNumberRequestCB(nil);
    }
}

- (void)onSmsCodeRequest:(NSObject<ConfirmPasswordOperation> *)smsCode {
    RCTLogInfo(@"onSmsCodeRequest");
    if (smsCode && self.onSmsCodeRequestCB) {
        self.userInputDispatch = ^(NSString *input) {
          if ([smsCode conformsToProtocol:@protocol(ConfirmPasswordOperation)]) {
              [smsCode confirmPassword:input];
          }
        };
        self.onSmsCodeRequestCB(nil);
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
    RCTLogInfo(@"got UserInput %@", val);
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

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onPhoneNumberRequest : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onPhoneNumberRequestCB = cb;
}

RCT_EXPORT_METHOD(set_cardlinkInteractionCB_onSmsCodeRequest : (RCTResponseSenderBlock)cb) {
    if (!clInteraction) {
        clInteraction = [CardLinkInterActionImp new];
    }
    clInteraction.onSmsCodeRequestCB = cb;
}

RCTResponseSenderBlock onSdkError;
RCT_EXPORT_METHOD(set_sdkErrorCB : (RCTResponseSenderBlock)cb) {
    if (!errHandler) {
        errHandler = [SdkErroHandler new];
    }
    errHandler.onSdkErrorCB = cb;
}

RCT_EXPORT_METHOD(getPrescriptions : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {

    dispatch_sync(dispatch_get_main_queue(), ^{
      if (clCtrlCB && clCtrlCB.prescriptionProtocol) {
          if ([clCtrlCB.prescriptionProtocol conformsToProtocol:@protocol(EpothekePrescriptionProtocol)]) {
              [clCtrlCB.prescriptionProtocol
                  requestPrescriptionsReq:[[EpothekeRequestPrescriptionList alloc] initWithIccsns:[NSArray new] messageId:@""]
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

RCT_EXPORT_METHOD(startCardLink : (NSString *)cardLinkUrl) {
    RCTLogInfo(@"onStarted: %@", cardLinkUrl);

    IOSNFCOptions *nfcOpts = [IOSNFCOptions new];

    EpothekeSdkCore *sdk = [[EpothekeSdkCore alloc] initWithCardLinkUrl:cardLinkUrl
                                             cardLinkControllerCallback:clCtrlCB
                                            cardLinkInteractionProtocol:clInteraction
                                                        sdkErrorHandler:errHandler
                                                                nfcOpts:nfcOpts];

    [sdk doInitCardLink];
}

@end
