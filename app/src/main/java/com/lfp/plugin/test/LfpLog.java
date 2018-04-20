package com.lfp.plugin.test;

import android.os.Build;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.concurrent.TimeUnit;

/**
 * Created by linfp on 2017/11/3.
 * 运行时注解编译器
 */

@Aspect
public class LfpLog {

    private static volatile boolean enabled = true;

    public static void setEnabled(boolean enabled) {
        LfpLog.enabled = enabled;
    }

    private static void enterMethod(JoinPoint joinPoint) {
        if (!enabled) return;

        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();

        Class<?> cls = codeSignature.getDeclaringType();
        String methodName = codeSignature.getName();
        String[] parameterNames = codeSignature.getParameterNames();
        Object[] parameterValues = joinPoint.getArgs();

        StringBuilder builder = new StringBuilder("\u21E2 ");
        builder.append(methodName).append('(');
        for (int i = 0; i < parameterValues.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(parameterNames[i]).append('=');
            builder.append(Strings.toString(parameterValues[i]));
        }
        builder.append(')');

        if (Looper.myLooper() != Looper.getMainLooper()) {
            builder.append(" [Thread:\"").append(Thread.currentThread().getName()).append("\"]");
        }

        Log.v(asTag(cls), builder.toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final String section = builder.toString().substring(2);
            Trace.beginSection(section);
        }
    }

    private static void exitMethod(JoinPoint joinPoint, Object result, long lengthMillis) {
        if (!enabled) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.endSection();
        }

        Signature signature = joinPoint.getSignature();

        Class<?> cls = signature.getDeclaringType();
        String methodName = signature.getName();
        boolean hasReturnType = signature instanceof MethodSignature
                && ((MethodSignature) signature).getReturnType() != void.class;

        StringBuilder builder = new StringBuilder("\u21E0 ")
                .append(methodName)
                .append(" [")
                .append(lengthMillis)
                .append("ms]");

        if (hasReturnType) {
            builder.append(" = ");
            builder.append(Strings.toString(result));
        }

        Log.v(asTag(cls), builder.toString());
    }

    private static String asTag(Class<?> cls) {
        if (cls.isAnonymousClass()) {
            return asTag(cls.getEnclosingClass());
        }
        return cls.getSimpleName();
    }

    @Pointcut("within(@com.lfp.plugin.test.DebugLog *)")
    public void withinAnnotatedClass() {
    }

    @Pointcut("execution(!synthetic * *(..)) && withinAnnotatedClass()")
    public void methodInsideAnnotatedType() {
    }

//	@Around("method()")
//	public Object loginAndExecute(ProceedingJoinPoint joinPoint) throws Throwable {
//		Object object = joinPoint.getTarget();
//		Context context = null;
//		if (object instanceof Activity) {
//			context = (Context) object;
//		}
//		if (object instanceof Fragment) {
//			context = ((Fragment) object).getActivity();
//		}
//		if (object instanceof android.support.v4.app.Fragment) {
//			context = ((android.support.v4.app.Fragment) object).getContext();
//		}
//		if (context != null) {
//			SPUtil spUtil = SPUtil.getInstance(context.getApplicationContext());
//			String loginId = (String) spUtil.get("login_id", "0");   //login_id是保存的用户凭证
//			String loginPhone = (String) spUtil.get("login_phone", "0");   //login_id是保存的用户凭证
//			if (TextUtils.equals("0", loginId) || TextUtils.equals("0", loginPhone)) {
//				Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show();
//				ARouter.getInstance().build("/user/login/").navigation(context);
////				return;
//			}
//		}
//		return joinPoint.proceed();
//	}

    @Pointcut("execution(!synthetic *.new(..)) && withinAnnotatedClass()")
    public void constructorInsideAnnotatedType() {
    }

    @Around("method() || constructor()")
    public Object logAndExecute(ProceedingJoinPoint joinPoint) throws Throwable {
        enterMethod(joinPoint);

        long startNanos = System.nanoTime();
        Object result = joinPoint.proceed();
        long stopNanos = System.nanoTime();
        long lengthMillis = TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos);

        exitMethod(joinPoint, result, lengthMillis);

        return result;
    }

    @Pointcut("execution(@com.lfp.plugin.test.DebugLog * *(..)) || methodInsideAnnotatedType()")
    public void method() {
    }

    @Pointcut("execution(@com.lfp.plugin.test.DebugLog *.new(..)) || constructorInsideAnnotatedType()")
    public void constructor() {
    }

//	@Around("method() || constructor()")
//	public Object logAndExecute(ProceedingJoinPoint joinPoint) throws Throwable {
//		Object object = joinPoint.getTarget();
//		Context context = null;
//		if (object instanceof Activity) {
//			context = (Context) object;
//		}
//		if (object instanceof Fragment) {
//			context = ((Fragment) object).getActivity();
//		}
//		if (object instanceof android.support.v4.app.Fragment) {
//			context = ((android.support.v4.app.Fragment) object).getContext();
//		}
//
//		if (context != null) {
//			SPUtil spUtil = SPUtil.getInstance(context.getApplicationContext());
//			String loginId = (String) spUtil.get("login_id", "0");   //login_id是保存的用户凭证
//			String loginPhone = (String) spUtil.get("login_phone", "0");   //login_id是保存的用户凭证
//			if (TextUtils.equals("0", loginId) || TextUtils.equals("0", loginPhone)) {
//				Toast.makeText(context, "请先登录" + loginId + "," + loginPhone, Toast.LENGTH_SHORT).show();
//				ARouter.getInstance().build("/user/login/").navigation(context);
//			}
//		}
//		return joinPoint.proceed();
//	}
}
