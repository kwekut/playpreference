package models.mail

import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.util.ArrayList
import java.util.Arrays
import play.api.Logger
import com.typesafe.config.ConfigFactory
import play.twirl.api.Html
import scala.util.Try
import scala.util.{Success, Failure}
import models._

case class Mail(
	title: String, 
	message: String, 
	about: String = "Kaosk provides security and convinience", 
	warning: String = "Warning: Do not ever send any sensitive via email and do not ever follow a link from and email to reset or create an account."
	) {

val mail = 
	raw"""<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	    <!--[if !mso]><!-->
	        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
	    <!--<![endif]-->
	    <meta name="viewport" content="width=device-width, initial-scale=1.0">
	    <title></title>
	<style type="text/css">


	@media screen and (max-width: 400px) {
	.two-column .column,
	    .three-column .column {
	        max-width: 100% !important;
	    }
	    .two-column img {
	        max-width: 100% !important;
	    }
	    .three-column img {
	        max-width: 50% !important;
	    }
	}
	body {
	    margin: 0 !important;
	    padding: 0;
	    background-color: #ffffff;
	}
	table {
	    border-spacing: 0;
	    font-family: sans-serif;
	    color: #333333;
	}
	td {
	    padding: 0;
	}
	img {
	    border: 0;
	}
	div[style*="margin: 16px 0"] { 
	    margin:0 !important;
	}
	.wrapper {
	    width: 100%;
	    table-layout: fixed;
	    -webkit-text-size-adjust: 100%;
	    -ms-text-size-adjust: 100%;
	}
	.webkit {
	    max-width: 600px;
	    margin: 0 auto;
	}
	.outer {
	Margin: 0 auto;
	    width: 100%;
	    max-width: 600px;
	}
	.full-width-image img {
	    width: 100%;
	    max-width: 600px;
	    height: auto;
	}
	.inner {
	padding: 10px;
	}
	p {
	    Margin: 0;
	}
	a {
	    color: #ee6a56;
	    text-decoration: underline;
	}
	.h1 {
	    font-size: 21px;
	    font-weight: bold;
	    Margin-bottom: 18px;
	}
	.h2 {
	    font-size: 18px;
	    font-weight: bold;
	    Margin-bottom: 12px;
	}
	 

	.one-column .contents {
	    text-align: left;
	}
	.one-column p {
	    font-size: 14px;
	    Margin-bottom: 10px;
	}

	.two-column {
	text-align: center;
	    font-size: 0;
	}
	.two-column .column {
	width: 100%;
	    max-width: 300px;
	    display: inline-block;
	    vertical-align: top;
	}
	.contents {
	width: 100%;
	}
	.two-column .contents {
	font-size: 14px;
	    text-align: left;
	}
	.two-column img {
	    width: 100%;
	    max-width: 280px;
	    height: auto;
	}
	.two-column .text {
	    padding-top: 10px;
	}    
	</style>
	    <!--[if (gte mso 9)|(IE)]>
	    <style type="text/css">
	        table {border-collapse: collapse !important !important;}
	    </style>
	    <![endif]-->
	</head>
	<body style="margin-top:0 !important;margin-bottom:0 !important;margin-right:0 !important;margin-left:0 !important;padding-top:0;padding-bottom:0;padding-right:0;padding-left:0;background-color:#ffffff;" >
	    <center class="wrapper" style="width:100%;table-layout:fixed;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%;" >
	        <div class="webkit" style="max-width:600px;margin-top:0;margin-bottom:0;margin-right:auto;margin-left:auto;" >
	<!--[if (gte mso 9)|(IE)]>
	<table width="600" align="center" style="border-spacing:0;font-family:sans-serif;color:#333333;" >
	<tr>
	<td style="padding-top:0;padding-bottom:0;padding-right:0;padding-left:0;" >
	<![endif]-->
	<table class="outer" align="center" style="border-spacing:0;font-family:sans-serif;color:#333333;Margin:0 auto;width:100%;max-width:600px;" >
	<tr>
	    <td class="full-width-image" style="padding-top:0;padding-bottom:0;padding-right:0;padding-left:0;" >
	        <img src="images/header.jpg" width="600" alt="" style="border-width:0;width:100%;max-width:600px;height:auto;" />
	    </td>
	</tr>

	<tr>
	<td class="one-column" style="padding-top:0;padding-bottom:0;padding-right:0;padding-left:0;" >
	        <table width="100%" style="border-spacing:0;font-family:sans-serif;color:#333333;" >
	            <tr>
	                <td class="inner contents" style="padding-top:10px;padding-bottom:10px;padding-right:10px;padding-left:10px;width:100%;text-align:left;" >
	                    <p class="h1" style="Margin:0;font-weight:bold;font-size:14px;Margin-bottom:10px;" >${title}</p>
	                    <p style="Margin:0;font-size:14px;Margin-bottom:10px;" >${message}</p>
	                </td>
	            </tr>
	        </table>
	    </td>
	</tr>

	<tr>
	<td class="two-column" style="padding-top:0;padding-bottom:0;padding-right:0;padding-left:0;text-align:center;font-size:0;" >
	        <!--[if (gte mso 9)|(IE)]>
	        <table width="100%" style="border-spacing:0;font-family:sans-serif;color:#333333;" >
	        <tr>
	        <td width="50%" valign="top" style="padding-top:0;padding-bottom:0;padding-right:0;padding-left:0;" >
	        <![endif]-->
	        <div class="column" style="width:100%;max-width:300px;display:inline-block;vertical-align:top;" >
	        <table width="100%" style="border-spacing:0;font-family:sans-serif;color:#333333;" >
	                <tr>
	                    <td class="inner" style="padding-top:10px;padding-bottom:10px;padding-right:10px;padding-left:10px;" >
	<table class="contents" style="border-spacing:0;font-family:sans-serif;color:#333333;width:100%;font-size:14px;text-align:left;" >
	    <tr>
	            <td style="padding-top:0;padding-bottom:0;padding-right:0;padding-left:0;" >
	                    <img src="images/two-column-01.jpg" width="280" alt="" style="border-width:0;width:100%;max-width:280px;height:auto;" />
	            </td>
	    </tr>
	    <tr>
	            <td class="text" style="padding-bottom:0;padding-right:0;padding-left:0;padding-top:10px;" >
	                    ${warning} 
	            </td>
	    </tr>
	</table>
	                    </td>
	                </tr>
	            </table>
	        </div>
	        <!--[if (gte mso 9)|(IE)]>
	        </td><td width="50%" valign="top" style="padding-top:0;padding-bottom:0;padding-right:0;padding-left:0;" >
	        <![endif]-->
	        <div class="column" style="width:100%;max-width:300px;display:inline-block;vertical-align:top;" >
	        <table width="100%" style="border-spacing:0;font-family:sans-serif;color:#333333;" >
	        <tr>
	        <td class="inner" style="padding-top:10px;padding-bottom:10px;padding-right:10px;padding-left:10px;" >
	        <table class="contents" style="border-spacing:0;font-family:sans-serif;color:#333333;width:100%;font-size:14px;text-align:left;" >
	        <tr>
	            <td style="padding-top:0;padding-bottom:0;padding-right:0;padding-left:0;" >
	                <img src="images/two-column-01.jpg" width="280" alt="" style="border-width:0;width:100%;max-width:280px;height:auto;" />
	            </td>
	        </tr>
	        <tr>
	            <td class="text" style="padding-bottom:0;padding-right:0;padding-left:0;padding-top:10px;" >
	                ${about} 
	            </td>
	        </tr>
	        </table>
	        </td>
	        </tr>
	            </table>
	        </div>
	        <!--[if (gte mso 9)|(IE)]>
	        </td>
	        </tr>
	        </table>
	        <![endif]-->
	    </td>
	</tr>
	</table>
	<!--[if (gte mso 9)|(IE)]>
	</td>
	</tr>
	</table>
	<![endif]-->
	        </div>
	    </center>
	</body>
	</html>"""

}