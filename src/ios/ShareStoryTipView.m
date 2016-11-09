//
//  ShareStoryTipView.m
//  ShareToStoryPosted
//
//  Created by limin on 16/11/8.
//  Copyright © 2016年 limin. All rights reserved.
//

#import "ShareStoryTipView.h"


@implementation VAUILabel
@synthesize verticalAlignment = verticalAlignment_;

- (id)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.verticalAlignment = VerticalAlignmentMiddle;
    }
    return self;
}

- (void)setVerticalAlignment:(VerticalAlignment)verticalAlignment {
    verticalAlignment_ = verticalAlignment;
    [self setNeedsDisplay];
}

- (CGRect)textRectForBounds:(CGRect)bounds limitedToNumberOfLines:(NSInteger)numberOfLines {
    CGRect textRect = [super textRectForBounds:bounds limitedToNumberOfLines:numberOfLines];
    switch (self.verticalAlignment) {
        case VerticalAlignmentTop:
            textRect.origin.y = bounds.origin.y;
            break;
        case VerticalAlignmentBottom:
            textRect.origin.y = bounds.origin.y + bounds.size.height - textRect.size.height;
            break;
        case VerticalAlignmentMiddle:
            // Fall through.
        default:
            textRect.origin.y = bounds.origin.y + (bounds.size.height - textRect.size.height) / 2.0;
    }
    return textRect;
}

-(void)drawTextInRect:(CGRect)requestedRect {
    CGRect actualRect = [self textRectForBounds:requestedRect limitedToNumberOfLines:self.numberOfLines];
    [super drawTextInRect:actualRect];
}


@end

@implementation ShareStoryTipView
@synthesize imageView,titleLbl,block;


+ (ShareStoryTipView *)sharedManager
{
    static ShareStoryTipView *shareStoryTipView = nil;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        shareStoryTipView = [[self alloc] init];
    });
    
    return shareStoryTipView;
}


-(instancetype)init{
    self = [super init];
    if (self) {
        self.frame =CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGH);
        self.backgroundColor=[UIColor clearColor];
        
        //背景
        UIView *bgView=[[UIView alloc]initWithFrame:self.frame];
        bgView.alpha=0.5;
        bgView.backgroundColor=[UIColor grayColor];
        [self addSubview:bgView];
        
        //弹框
        UIView *centerView=[[UIView alloc]initWithFrame:CGRectMake(20, 0, SCREEN_WIDTH-40, Center_title+Center_center+Center_down)];
        centerView.center = self.center;
        centerView.alpha=1;
        centerView.layer.masksToBounds=YES;
        centerView.layer.cornerRadius = 15;
        centerView.backgroundColor=[UIColor whiteColor];
        
        
        //题目
        UILabel *lable = [[UILabel alloc]initWithFrame:CGRectMake(0, 0, centerView.frame.size.width, Center_title)];
        lable.text=@"故事贴";
        lable.textAlignment=NSTextAlignmentCenter;
        lable.font = [UIFont fontWithName:@"Helvetica-Bold" size:20];
        [centerView addSubview:lable];
        
        //上线
        UIView *line1=[[UIView alloc]initWithFrame:CGRectMake(0, Center_title, centerView.frame.size.width, 0.5)];
        line1.backgroundColor=[UIColor grayColor];
        line1.alpha=0.3;
        [centerView addSubview:line1];
        
        //下线
        UIView *line2=[[UIView alloc]initWithFrame:CGRectMake(0, Center_title+Center_center, centerView.frame.size.width, 0.5)];
        line2.backgroundColor=[UIColor grayColor];
        line2.alpha=0.3;
        [centerView addSubview:line2];
        
        //图
        self.imageView = [[UIImageView alloc]initWithFrame:CGRectMake(10, lable.frame.size.height+10, Center_center-20, Center_center-20)];
        self.imageView .backgroundColor=[UIColor colorWithWhite:0.0f alpha:0.1];;
        [centerView addSubview:self.imageView];
        
        //文章题目
        self.titleLbl=[[VAUILabel alloc]initWithFrame:CGRectMake(20+self.imageView.frame.size.width, lable.frame.size.height+ 10, centerView.frame.size.width-Center_center, Center_center-20)];
        self.titleLbl.verticalAlignment = VerticalAlignmentTop;
        self.titleLbl.text = @"";
        self.titleLbl.font=[UIFont systemFontOfSize:16];
        self.titleLbl.lineBreakMode = NSLineBreakByTruncatingTail;
        self.titleLbl.numberOfLines = 0;
        
        [centerView addSubview:self.titleLbl];
        
        //取消按钮
        UIButton *cencel=[[UIButton alloc]initWithFrame:CGRectMake(0, 0, Center_title,Center_title)];
        //        [cencel setTitle:@"取消" forState:UIControlStateNormal];
        UILabel *lbl=[[UILabel alloc]initWithFrame:CGRectMake(10, 0, Center_title, Center_title)];
        lbl.font=[UIFont systemFontOfSize:15];
        lbl.text=@"取消";
        //5，188，9
        lbl.textColor = [UIColor colorWithRed:5/255.0 green:188/255.0 blue:9/255.0 alpha:1];
        //lbl.textColor=[UIColor greenColor];
        //        cencel setTitleColor:[UIColor whiteColor] forState:<#(UIControlState)#>
        [cencel addTarget:self action:@selector(cencle) forControlEvents:UIControlEventTouchUpInside];
        [cencel addSubview:lbl];
        [centerView addSubview:cencel];
        
        //分享按钮
        UIButton *share=[[UIButton alloc]initWithFrame:CGRectMake(0, Center_title+Center_center, 150, Center_down)];
        //        [share setTitle:@"分享到故事贴群" forState:UIControlStateNormal];
        UILabel *lbl1=[[UILabel alloc]initWithFrame:CGRectMake(10, Center_title+Center_center, 150, Center_down)];
        lbl1.font=[UIFont systemFontOfSize:16];
        lbl1.text=@"分享到故事贴群";
        lbl1.textColor = [UIColor colorWithRed:5/255.0 green:188/255.0 blue:9/255.0 alpha:1];
        [cencel addSubview:lbl1];
        [share addTarget:self action:@selector(share) forControlEvents:UIControlEventTouchUpInside];
        [centerView addSubview:share];
        
        [self addSubview:centerView];
        
        
    }
    return self;
}

-(void)cencle{
    [self removeFromSuperview];
}

-(void)share{
    self.block();
    [self cencle];
}

@end


