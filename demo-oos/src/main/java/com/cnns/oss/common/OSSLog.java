package com.cnns.oss.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OSSLog {

	Logger logger = LoggerFactory.getLogger(OSSLog.class);
	
	@Pointcut("execution(* com.cnns.oss.service.*.*(..))")
	public void join() {
	
	}
	@Around("join()")
	public void around(ProceedingJoinPoint point) {
		//通过signature得到目标方法的包.类.方法名
		Signature signature = point.getSignature();
		logger.info("执行目标方法开始:{}",signature);
		try {
			point.proceed();
		} catch (Throwable e) {
			logger.error("throw an error",e);
			new RuntimeException(e);
		}
		logger.info("执行目标方法结束:{}",signature);
	}
}
