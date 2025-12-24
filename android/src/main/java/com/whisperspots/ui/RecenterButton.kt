package com.whisperspots.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class RecenterButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var isPressed = false
    private var isVisible = false
    private var animatedAlpha = 0f
    
    var onButtonClick: (() -> Unit)? = null
    
    init {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        
        shadowPaint.color = Color.parseColor("#33000000")
        shadowPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        
        iconPaint.color = Color.parseColor("#007AFF")
        iconPaint.strokeWidth = 3f
        iconPaint.strokeCap = Paint.Cap.ROUND
        iconPaint.style = Paint.Style.STROKE
        
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        
        isClickable = true
        isFocusable = true
        alpha = 0f
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = 48.dpToPx()
        setMeasuredDimension(size, size)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (animatedAlpha <= 0f) return
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width / 2f) - 10f
        
        canvas.save()
        canvas.scale(animatedAlpha, animatedAlpha, centerX, centerY)
        
        if (!isPressed) {
            canvas.drawCircle(centerX, centerY + 2f, radius, shadowPaint)
        }
        
        val scale = if (isPressed) 0.95f else 1f
        canvas.save()
        canvas.scale(scale, scale, centerX, centerY)
        
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        val compassSize = radius * 0.5f
        
        canvas.drawCircle(centerX, centerY, compassSize * 0.3f, iconPaint)
        
        val arrowPath = Path().apply {
            moveTo(centerX, centerY - compassSize)
            lineTo(centerX - compassSize * 0.3f, centerY - compassSize * 0.5f)
            moveTo(centerX, centerY - compassSize)
            lineTo(centerX + compassSize * 0.3f, centerY - compassSize * 0.5f)
        }
        canvas.drawPath(arrowPath, iconPaint)
        
        canvas.restore()
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || !isVisible) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                performHapticFeedback()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isPressed) {
                    isPressed = false
                    invalidate()
                    onButtonClick?.invoke()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    fun show(animated: Boolean = true) {
        if (isVisible) return
        
        isVisible = true
        visibility = VISIBLE
        
        if (animated) {
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 250
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addUpdateListener {
                animatedAlpha = it.animatedValue as Float
                invalidate()
            }
            animator.start()
        } else {
            animatedAlpha = 1f
            invalidate()
        }
    }
    
    fun hide(animated: Boolean = true) {
        if (!isVisible) return
        
        isVisible = false
        
        if (animated) {
            val animator = ValueAnimator.ofFloat(animatedAlpha, 0f)
            animator.duration = 250
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addUpdateListener {
                animatedAlpha = it.animatedValue as Float
                invalidate()
            }
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    visibility = GONE
                }
            })
            animator.start()
        } else {
            animatedAlpha = 0f
            visibility = GONE
        }
    }
    
    private fun performHapticFeedback() {
        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
    
    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
