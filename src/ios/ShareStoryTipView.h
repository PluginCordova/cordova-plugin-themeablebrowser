//
//  ShareStoryTipView.h
//  ShareToStoryPosted
//
//  Created by limin on 16/11/8.
//  Copyright © 2016年 limin. All rights reserved.
//

#import <UIKit/UIKit.h>

#define SCREEN_WIDTH ([UIScreen mainScreen].bounds.size.width)
#define SCREEN_HEIGH ([UIScreen mainScreen].bounds.size.height)

#define Center_title    40  //题目高
#define Center_center   80  //内容高
#define Center_down     50  //底部高


typedef enum
{
    VerticalAlignmentTop = 0, // default
    VerticalAlignmentMiddle,
    VerticalAlignmentBottom,
} VerticalAlignment;
@interface VAUILabel : UILabel
{
@private
    VerticalAlignment _verticalAlignment;
}

@property (nonatomic) VerticalAlignment verticalAlignment;

@end


@interface ShareStoryTipView : UIView

@property (nonatomic,copy)void(^block)(void);

@property (nonatomic,strong)VAUILabel *titleLbl;

@property (nonatomic,strong)UIImageView *imageView;


+ (ShareStoryTipView *)sharedManager;

@end


