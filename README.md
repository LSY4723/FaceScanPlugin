# FaceScanPlugin
cordova人脸识别插件，目前兼容有问题，我是初学cordova，所有不精细，有会的希望补充一下，或者帮忙纠正错误，我知道有错误的部分



1.	集成cordova 人脸识别插件
1.解压FaceScanPlugin.jar,将文件拷入Wex5的Native/plugins 下
2.	 如果开发为cordova环境下，可以手动添加本地plugin。命令如下：
Cordova plugin add FaceScanPlugin的文件根目录位置
3.	如何使用：
Wex5下,导入该插件：
require("cordova!FaceScanPlugin");
在触发该事件后：
声明一个空参数组，因为cordova在执行exec时候要求必须传该参数,而该参数在本插件中不起作用，所有需要声明空参
如：var arges=[];
然后执行该插件人脸扫描的方法：
cordova.plugins.FacePlugin.faceScan(arges,success , error);
success 为扫描成功后回调函数，error为失败后的回调函数
4.	例子：
define(function(require){
	var $ = require("jquery");
	var justep = require("$UI/system/lib/justep");
	require("cordova!FaceScanPlugin");
	var Model = function(){
		this.callParent();
		this.modelLoad;
	};
	Model.prototype.modelLoad = function(event){
		
	};
	Model.prototype.button1Click = function(event){
		console.log(cordova.plugins.FacePlugin.faceScan);
		var arges=[];
		cordova.plugins.FacePlugin.faceScan(arges,success , error);
		function success(msg){
			alert(msg);
			  $.ajax({
					 global: false, // 此处设为false，即可
		             type: "POST",
		             contentType: "application/json",
		             dataType: "json",
		             url:"http://****/*** /SystemManagerController/loginByFace",
		             data: JSON.stringify({base :msg}),
		             success: function(data){
		            	 console.log(data);
		            	 justep.Util.hint("扫描成功！", {
							"delay" : "2000",
							"position":"bottom",
							"style" :"text-align:center;color:#fff;background-image : -webkit-linear-gradient(top, #000 0, #000 100%);opacity:1;border:none;"
				        });
		             }
		        });
		}

		function error(msg){
			console.log(msg);
			alert(msg);
		}
	};
	return Model;
});
5.	扫描成功后返回base64图片,然后ajax调用后台接口
 正式环境：
http://****/***/SystemManagerController/loginByFace
测试环境：
http://****/****/SystemManagerController/loginByFace
6.	接口调用采用application/json  post请求
      data: JSON.stringify({base :msg})
传人的参数
base	Base64图片
7.	响应的接口数据：
 
 

