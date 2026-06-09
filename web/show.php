<?php
$counterFile = 'download_count.txt';
$count = file_exists($counterFile) ? file_get_contents($counterFile) : 0;
echo "<div style='text-align:center; margin-top:50px; font-family: sans-serif;'>";
echo "<h2>SimpleSchedule 课表更新服务</h2>";
echo "<h3>安装包已被下载：<span style='color:#dc2626; font-size: 24px;'> " . $count . " </span> 次</h3>";
echo "</div>";
?>
