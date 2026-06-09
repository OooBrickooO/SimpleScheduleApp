<?php
$counterFile = 'download_count.txt';
$count = 0;
if (file_exists($counterFile)) {
    $count = (int)file_get_contents($counterFile);
}
$count++;
file_put_contents($counterFile, $count);

// 重定向到网站目录下的真实安装包
header('Location: https://www.lingflame.cn/app-release.apk');
exit;
?>
