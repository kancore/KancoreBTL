package com.kancoreBC.kancorektl

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

class KanJoypadView: SurfaceView, SurfaceHolder.Callback, View.OnTouchListener{


    //the center of the sticks
    var centerX: Float = 0.toFloat()
    var centerY: Float = 0.toFloat()
    var baseRadius: Float = 0.toFloat() //The radius of the base circle
    var hatRadius: Float = 0.toFloat() //Radius of the hat circle
    val colors: Paint = Paint(Paint.ANTI_ALIAS_FLAG) //Do not touch this
    var displacement: Float = 0.toFloat() //Used for constraint the joystick
    var joypadCallback: JoypadListener? = null



    //gets the 0 positions and the radius of the joypad
    fun setupVariables(){
        centerX = (width / 2).toFloat()
        centerY = (height / 2).toFloat()
        baseRadius = (Math.min(width, height) / 3.5).toFloat()
        hatRadius = (Math.min(width, height) / 6).toFloat()
    }

    //draws the joypad
    fun drawJoypad(newX: Float, newY: Float){
        if(getHolder().surface.isValid){



                var joypadCanvas: Canvas = this.getHolder().lockCanvas()
                //makes the background of the view transparent
                joypadCanvas.drawColor(0, PorterDuff.Mode.CLEAR)


                //draw the base
                //setSolidColor(R.color.primary)

                colors.setARGB(255, 17, 0, 28)
                joypadCanvas.drawCircle(centerX, centerY, baseRadius, colors)

                //draw the hat
                //setSolidColor(R.color.primary_light)
                colors.setARGB(200, 0, 100, 148)
                joypadCanvas.drawCircle(newX, newY, hatRadius, colors)

                //print and show the joypad
                this.getHolder().unlockCanvasAndPost(joypadCanvas)

        }
    }
    //To be implemented
    fun setSolidColor(colorIn: Int) {
        //color = Color.parseColor(colorIn)
        colors.setColor(colorIn)
    }

    //the interface for the main functionally of the view
    interface JoypadListener {
        fun onJoypadMove(xPercent: Float, yPercent: Float, src: Int){}
    }


    //the main constructor for the class, do not change nothing below this
    constructor(context: Context): super(context){
        this.setBackgroundColor(Color.TRANSPARENT)
        this.setZOrderOnTop(true)
        getHolder().setFormat(PixelFormat.TRANSPARENT)
        getHolder().addCallback(this)
        setOnTouchListener(this)
    }

    constructor(context: Context, attrs: AttributeSet,): super(context, attrs){
        this.setBackgroundColor(Color.TRANSPARENT)
        this.setZOrderOnTop(true)
        getHolder().setFormat(PixelFormat.TRANSPARENT)
        getHolder().addCallback(this)
        setOnTouchListener(this)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle){
        this.setBackgroundColor(Color.TRANSPARENT)
        this.setZOrderOnTop(true)
        getHolder().setFormat(PixelFormat.TRANSPARENT)
        getHolder().addCallback(this)
        setOnTouchListener(this)
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        //gets the Zero zone of the joypad. Radius refers to the max displacement distant of the base circle and the hat circle
        setupVariables()
        drawJoypad(centerX, centerY)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {

    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        //Log.i("jpadView:", ""+view)
        if(view != null) {
            if (view.equals(this)) { //checks the user is touching the joypadview
                if (event!!.action != MotionEvent.ACTION_UP){ //checks the user is not lifting his finger
                    //calculates the total distance happening with the touch
                    displacement = Math.sqrt(
                        Math.pow((event.x - centerX).toDouble(),
                            2.toDouble()) + Math.pow((event.y - centerY).toDouble(),
                            2.toDouble())).toFloat()

                    //checks if the touching is happening inside of the limits of the base
                    if (displacement <= baseRadius) {
                        //Draws the joypad with the new positions
                        drawJoypad(event.x, event.y)
                        if(joypadCallback != null) {
                            joypadCallback!!.onJoypadMove(
                                (event.x - centerX) / baseRadius,
                                (event.y - centerX) / baseRadius,
                                id
                            )
                        } else {
                            Log.i("Joypad", "Callback is null")
                        }
                    } else {
                        //Then the user is touching outside of the borders of the Joypad
                        //lets this action to still happen but lets draws the hat inside of the borders of the base
                        var ratio = baseRadius / displacement
                        var constrainedX: Float = centerX + (event.x - centerX) * ratio
                        var constrainedY: Float = centerY + (event.y - centerY) * ratio
                        drawJoypad(constrainedX, constrainedY)
                        if(joypadCallback != null) {
                            joypadCallback!!.onJoypadMove(
                                (constrainedX - centerX) / baseRadius,
                                (constrainedY - centerX) / baseRadius,
                                id
                            )
                        } else {
                            Log.i("Joypad", "Callback is null")
                        }
                        //uncomment for debug purposes only
                        /*Log.i("radius", ""+baseRadius)
                        Log.i("displacement",""+displacement)
                        Log.i("ratio",ratio.toString())
                        Log.i("Y", event.y.toString())
                        Log.i("ConstrainedY", constrainedY.toString())*/
                    }
                }
            }
            if (event!!.action == MotionEvent.ACTION_UP) { //resets the joypad to the center
                drawJoypad(centerX, centerY)
                if(joypadCallback != null) {
                    joypadCallback!!.onJoypadMove(0.toFloat(), 0.toFloat(), id)
                } else {
                    Log.i("Joypad callback is null", "Callback is null")
                }
            }
        }
        return true
    }
}