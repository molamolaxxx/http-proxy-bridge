#!/bin/bash

count=`ps -ef |grep "http-proxy-forward" |grep -v "grep" |wc -l`
if [ $count -gt 0 ]; then
    if [ -f "$pid_file" ]; then
        pid=`cat $pid_file`
        kill -15 $pid
    else
        pid=`ps -ef |grep "http-proxy-forward" |grep -v "grep"| awk '{print $2}'`
        kill -15 $pid
    fi
fi

count=`ps -ef |grep "http-proxy-reverse" |grep -v "grep" |wc -l`
if [ $count -gt 0 ]; then
    if [ -f "$pid_file" ]; then
        pid=`cat $pid_file`
        kill -15 $pid
    else
        pid=`ps -ef |grep "http-proxy-reverse" |grep -v "grep"| awk '{print $2}'`
        kill -15 $pid
    fi
fi

count=`ps -ef |grep "http-proxy-encryption" |grep -v "grep" |wc -l`
if [ $count -gt 0 ]; then
    if [ -f "$pid_file" ]; then
        pid=`cat $pid_file`
        kill -15 $pid
    else
        pid=`ps -ef |grep "http-proxy-encryption" |grep -v "grep"| awk '{print $2}'`
        kill -15 $pid
    fi
fi
