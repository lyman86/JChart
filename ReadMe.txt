打开后请按照AndroidManifest的提示替换您的包名和APPKey

导入Eclipse：

选中该项目按Ctrl + H（全局搜索） , 在弹出的对话框中选择File Search-->在Containing text中填写：
import cn.jpush.im.android.demo.R; （别忘了分号）

然后点击Replace在弹出的对话框中

全局替换：
import cn.jpush.im.android.demo.R;

替换为
import 您的包名.R;


最后Build 一下此项目即可运行。


导入Android Studio：
在你的Project中选择File->Import Module->在路径中选择你的工程包->finished

请按照提示修改AndroidManifest文件，并全局替换R文件，具体操作如下：

Edit->Find->Replace in Path->在Text to find中填写:import cn.jpush.im.android.demo.R;
在Replace with中填写：import 您的包名.R; 在Scope栏中，选中Module，选择JMessageDemo。最后确定即可

然后在菜单栏中选择Build->Clean Project然后Build Project（或者在右侧菜单中选择Gradle->找到JMessageDemo->双击clean，然后双击Build。

Tip:
若提示“Error:Cause: failed to find target android-19.0.1 Please install the missing platform from the Android SDK Manager”，请在JMessageDemo下的build.gradle文件中修改buildToolsVersion为你当前的版本。

