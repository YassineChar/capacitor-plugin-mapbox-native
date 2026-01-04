package com.whisperspots.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CreateWhisperButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var isPressed = false
    private var isLoading = false
    private var rotationAngle = 0f
    
    var onButtonClick: (() -> Unit)? = null
    
    init {
        paint.color = Color.parseColor("#007AFF")
        paint.style = Paint.Style.FILL
        
        shadowPaint.color = Color.parseColor("#33000000")
        shadowPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        
        iconPaint.color = Color.WHITE
        iconPaint.strokeWidth = 4f
        iconPaint.strokeCap = Paint.Cap.ROUND
        iconPaint.style = Paint.Style.STROKE
        
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        
        isClickable = true
        isFocusable = true
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = 64.dpToPx()
        setMeasuredDimension(size, size)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width / 2f) - 12f
        
        if (!isPressed) {
            canvas.drawCircle(centerX, centerY + 2f, radius, shadowPaint)
        }
        
        val scale = if (isPressed) 0.95f else 1f
        canvas.save()
        canvas.scale(scale, scale, centerX, centerY)
        
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        if (isLoading) {
            canvas.save()
            canvas.rotate(rotationAngle, centerX, centerY)
            
            val spinnerRadius = radius * 0.6f
            iconPaint.style = Paint.Style.STROKE
            iconPaint.strokeWidth = 3f
            
            val rect = RectF(
                centerX - spinnerRadius,
                centerY - spinnerRadius,
                centerX + spinnerRadius,
                centerY + spinnerRadius
            )
            canvas.drawArc(rect, 0f, 270f, false, iconPaint)
            
            canvas.restore()
        } else {
            val iconSize = radius * 0.5f
            
            canvas.drawLine(
                centerX - iconSize, centerY,
                centerX + iconSize, centerY,
                iconPaint
            )
            
            canvas.drawLine(
                centerX, centerY - iconSize,
                centerX, centerY + iconSize,
                iconPaint
            )
        }
        
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || isLoading) return false
        
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
    
    fun setLoading(loading: Boolean) {
        if (isLoading == loading) return
        
        isLoading = loading
        
        if (loading) {
            startSpinAnimation()
        } else {
            stopSpinAnimation()
        }
        
        invalidate()
    }
    
    private fun startSpinAnimation() {
        val animator = ObjectAnimator.ofFloat(this, "rotationAngle", 0f, 360f)
        animator.duration = 1000
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.addUpdateListener {
            rotationAngle = it.animatedValue as Float
            invalidate()
        }
        animator.start()
        setTag("spinner_animator_tag".hashCode(), animator)
    }
    
    private fun stopSpinAnimation() {
        val animator = getTag("spinner_animator_tag".hashCode()) as? ObjectAnimator
        animator?.cancel()
        setTag("spinner_animator_tag".hashCode(), null)
        rotationAngle = 0f
    }
    
    private fun performHapticFeedback() {
        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
    
    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
