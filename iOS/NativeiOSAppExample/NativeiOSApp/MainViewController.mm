#import <UIKit/UIKit.h>

#include "UVolPlayer.h"

//-UVolPlayer functions called with [[UVolPlayer uVolPlayer] methodname];
//
//call ARPlace("x, y")
//call Play("path")
//call Pause
//call PlayPauseToggle
//call StartRecording
//call StopRecording
//call TakeScreenshot
//call ShareCapture


void showAlert(NSString* title, NSString* msg) {
    UIAlertController* alert = [UIAlertController alertControllerWithTitle:title message:msg                                                         preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"Ok" style:UIAlertActionStyleDefault
                                                          handler:^(UIAlertAction * action) {}];
    [alert addAction:defaultAction];
    auto delegate = [[UIApplication sharedApplication] delegate];
    [delegate.window.rootViewController presentViewController:alert animated:YES completion:nil];
}
@interface MyViewController : UIViewController
@end

@interface AppDelegate : UIResponder<UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;
@property (nonatomic, strong) UIButton *playButton;
@property (nonatomic, strong) UIButton *playpauseButton;
@property (nonatomic, strong) UINavigationController *navVC;
@property (nonatomic, strong) UIButton *screenshotButton;
@property (nonatomic, strong) UIButton *startrecordingButton;
@property (nonatomic, strong) UIButton *stoprecordingButton;
@property (nonatomic, strong) UIButton *shareButton;

@property (nonatomic, strong) UIButton *quitBtn;
@property (nonatomic, strong) MyViewController *viewController;

@end

AppDelegate* hostDelegate = NULL;

// -------------------------------
// -------------------------------
// -------------------------------


@interface MyViewController ()
@property (nonatomic, strong) UITextField *filePathField;
@property (nonatomic, strong) UIButton *playBtn;
@property (nonatomic, strong) UIButton *playpauseBtn;
@property (nonatomic, strong) UIButton *screenshotBtn;
@property (nonatomic, strong) UIButton *startrecordBtn;
@property (nonatomic, strong) UIButton *stoprecordingBtn;
@property (nonatomic, strong) UIButton *shareBtn;

@property (nonatomic, strong) UIButton *quitBtn;
@end

@implementation MyViewController
- (void)viewDidLoad
{
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor blueColor];
    
    // PlayButton
    self.playBtn = [UIButton buttonWithType: UIButtonTypeSystem];
    [self.playBtn setTitle: @"Play" forState: UIControlStateNormal];
    self.playBtn.frame = CGRectMake(0, 0, 100, 44);
    self.playBtn.center = CGPointMake(50, 120);
    self.playBtn.backgroundColor = [UIColor greenColor];
    [self.playBtn addTarget: hostDelegate action: @selector(PlayUVolTest) forControlEvents: UIControlEventPrimaryActionTriggered];
    [self.view addSubview: self.playBtn];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

@end


// keep arg for unity init from non main
int gArgc = 0;
char** gArgv = nullptr;
NSDictionary* appLaunchOpts;


@implementation AppDelegate


- (void)PlayUVolTest
{
    NSLog(@"PlayUVolTest called");

    //showAlert(@"Test fuction", @"called");
    //[[UVolPlayer uVolPlayer] testMtd];
    [[UVolPlayer uVolPlayer] Play:(char*)[@"http://wildcapture.co/liam.drcs" UTF8String]];
    //[self performSelector:@selector(function:) withObject:@"myString"];
    //Sample Player UI
    auto unityView = [[UVolPlayer uVolPlayer] UnityView];//[[[self ufw] appController] rootView];
    if(self.playpauseButton == nil) {
        self.playpauseButton = [UIButton buttonWithType: UIButtonTypeSystem];
        [self.playpauseButton setTitle: @"PlayPause" forState: UIControlStateNormal];
        self.playpauseButton.frame = CGRectMake(0, 0, 100, 44);
        self.playpauseButton.center = CGPointMake(50, 300);
        self.playpauseButton.backgroundColor = [UIColor greenColor];
        [unityView addSubview: self.playpauseButton];
        [self.playpauseButton addTarget: [UVolPlayer uVolPlayer] action: @selector(PlayPauseToggle) forControlEvents: UIControlEventPrimaryActionTriggered];
        
        self.screenshotButton = [UIButton buttonWithType: UIButtonTypeSystem];
        [self.screenshotButton setTitle: @"Take Screenshot" forState: UIControlStateNormal];
        self.screenshotButton.frame = CGRectMake(0, 0, 100, 44);
        self.screenshotButton.center = CGPointMake(150, 300);
        self.screenshotButton.backgroundColor = [UIColor yellowColor];
        [unityView addSubview: self.screenshotButton];
        [self.screenshotButton addTarget: [UVolPlayer uVolPlayer] action: @selector(TakeScreenshot)  forControlEvents: UIControlEventPrimaryActionTriggered];
        
        self.startrecordingButton = [UIButton buttonWithType: UIButtonTypeSystem];
        [self.startrecordingButton setTitle: @"Start Recording" forState: UIControlStateNormal];
        self.startrecordingButton.frame = CGRectMake(0, 0, 100, 44);
        self.startrecordingButton.center = CGPointMake(150, 350);
        self.startrecordingButton.backgroundColor = [UIColor yellowColor];
        [unityView addSubview: self.startrecordingButton];
        [self.startrecordingButton addTarget: [UVolPlayer uVolPlayer] action: @selector(StartRecording)  forControlEvents: UIControlEventPrimaryActionTriggered];
        
        self.stoprecordingButton = [UIButton buttonWithType: UIButtonTypeSystem];
        [self.stoprecordingButton setTitle: @"Stop Recording" forState: UIControlStateNormal];
        self.stoprecordingButton.frame = CGRectMake(0, 0, 100, 44);
        self.stoprecordingButton.center = CGPointMake(150, 400);
        self.stoprecordingButton.backgroundColor = [UIColor yellowColor];
        [unityView addSubview: self.stoprecordingButton];
        [self.stoprecordingButton addTarget: [UVolPlayer uVolPlayer] action: @selector(StopRecording)  forControlEvents: UIControlEventPrimaryActionTriggered];
        
        self.shareButton = [UIButton buttonWithType: UIButtonTypeSystem];
        [self.shareButton setTitle: @"Share" forState: UIControlStateNormal];
        self.shareButton.frame = CGRectMake(250, 0, 100, 44);
        self.shareButton.center = CGPointMake(250, 300);
        self.shareButton.backgroundColor = [UIColor redColor];
        [self.shareButton addTarget: [UVolPlayer uVolPlayer] action: @selector(ShareCapture) forControlEvents: UIControlEventPrimaryActionTriggered];
        [unityView addSubview: self.shareButton];
        
        // Quit
        self.quitBtn = [UIButton buttonWithType: UIButtonTypeSystem];
        [self.quitBtn setTitle: @"Exit Player" forState: UIControlStateNormal];
        self.quitBtn.frame = CGRectMake(250, 0, 100, 44);
        self.quitBtn.center = CGPointMake(250, 350);
        self.quitBtn.backgroundColor = [UIColor redColor];
        [self.quitBtn addTarget: self action: @selector(ExitUVol) forControlEvents: UIControlEventPrimaryActionTriggered];
        [unityView addSubview: self.quitBtn];
    }
}
- (void)ExitUVol{
    [[UVolPlayer uVolPlayer] ExitPlayer];
    [self.window makeKeyAndVisible];
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    hostDelegate = self;
    appLaunchOpts = launchOptions;
    NSLog(@"mainview didFinishLaunchingWithOptions");
    self.window = [[UIWindow alloc] initWithFrame: [UIScreen mainScreen].bounds];
    self.window.backgroundColor = [UIColor redColor];
    //ViewController *viewcontroller = [[ViewController alloc] initWithNibName:nil Bundle:nil];
    self.viewController = [[MyViewController alloc] init];
    self.navVC = [[UINavigationController alloc] initWithRootViewController: self.viewController];
    self.window.rootViewController = self.navVC;
    [self.window makeKeyAndVisible];
    
    return YES;
}

@end


int main(int argc, char* argv[])
{
    gArgc = argc;
    gArgv = argv;
    
    @autoreleasepool
    {
      
            // run host app first and then unity later
            UIApplicationMain(argc, argv, nil, [NSString stringWithUTF8String: "AppDelegate"]);
        
    }
    
    return 0;
}
