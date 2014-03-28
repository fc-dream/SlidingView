# SlidingView #
SlidingView是一个从[SlidingLayer](https://github.com/6wunderkinder/android-sliding-layer-lib "SlidingLayer")那个fork来的项目。但是已经被我改的面目全非，所以干脆就重新开辟一个新的项目。建立这个项目的最初目的是想要在[SlidingLayer](https://github.com/6wunderkinder/android-sliding-layer-lib "SlidingLayer")的基础上写一个类似于[SlidingMenu](https://github.com/jfeinstein10/SlidingMenu "SlidingMenu")的UI组件。因为jfeinstein10的[SlidingMenu](https://github.com/jfeinstein10/SlidingMenu "SlidingMenu")有太多我不满意的地方，而修改起来又太麻烦。最终痛定思痛，决定从头写起。而SlidingView正是这个过程中的一个产物（好吧，其实这个时候，我的SlidingMenu八字还没有一撇了）。<br>

SlidingView为大家提供了一个能够高度自由滑动的UI组件。你可以为SlidingView添加任意多个方向不同的位置，以及滑动范围。之后你就可以拖拽她到达设定的位置，当然你也可以通过`switchPosition`切换她到设定的位置。
## 如何导入项目 ##
SlidingView使用了gradle作为构建工具。因此，如果你和我一样在使用AndroidStudio那么按照下面的步骤就可以轻松的导入我的项目。如果你依旧在使用 Eclipse，你可以google一下找到解决方案。  


1. 将sliding_view_library整个目录复制到你的项目下，使项目结构成为 
```
	YourProjet  
		|———YourModel  
		|———sliding_view_library  
		|———setting.gradle
```

2. 在`settings.gradle`文件中加入`include ':library'`

3. 修改`YourModel`的`build.gradle`文件的`dependencies`
```
	dependencies {
    	compile project(':sliding_view_library')
	}
```
## 快速上手 ##
使用SlidingView一共就两步

1. 在Layout文件中添加`SlidingView`，并设置所需参数。
2. 在代码中调用`addPosition`增加目标位置。

## 代码示例 ##
在Layout文件中

    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    	xmlns:app="http://schemas.android.com/apk/res-auto"
	    android:layout_width="match_parent"
	    android:layout_height="match_parent"
	    android:orientation="vertical"
	    android:background="@android:color/black">
	    <com.kohoh.SlidingView.SlidingView
		    android:layout_width="match_parent"
		    android:layout_height="match_parent"
		    android:id="@+id/mSlidingView"
		    app:initialX="0dp"
		    app:initialY="0dp"
		    app:slideEnable="true"
		    app:dragEnable="true"
		    app:interceptAllGestureEnable="false"
		    android:background="@android:color/darker_gray">
	    </com.kohoh.SlidingView.SlidingView>
    </LinearLayout>


在java代码中

    SlidingView slidingView=(SlidingView)findViewById(R.id.mSlidingView);
    slidingView.addPosition(POSITION_TOP,0,500);
    slidingView.addPosition(POSITION_BOTTOM,0,500);

## 联系方式 ##

如果你有什么想要吐槽的，或者不满的，或者使用过程中遇到的问题。甚或者想要帮助我一起开发这个项目的同学，欢迎联系我。

我的邮箱:qxw2012@hotmail.com


