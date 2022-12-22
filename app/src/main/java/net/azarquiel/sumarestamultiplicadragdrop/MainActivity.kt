package net.azarquiel.sumarestamultiplicadragdrop

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.core.view.isVisible
import net.azarquiel.sumarestamultiplicadragdrop.databinding.ActivityMainBinding
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.N)
class MainActivity : AppCompatActivity(), View.OnTouchListener, View.OnDragListener {

    companion object {
        const val MIN_DISTANCE_Y = 20
        const val TAG = "SumaDragDrop"
    }
    private var downY: Float = 0F
    private var movedY: Float = 0.0F
    private var deltaY: Float = 0.0F
    private var op1: Int = 0
    private var op2: Int = 0
    private val digitos = "0123456789"
    private val resultados = arrayOfNulls<Int>(3)
    private val operadores = intArrayOf(R.drawable.suma, R.drawable.resta, R.drawable.multi)
    private var cont1 = 1
    private var cont2 = 0
    private var mod3 = 0 //Operador módulo 3 -> Sirve para cambiar de modo cuando tienes 3 modos
    private var modR = 0 //Operador módulo R -> Donde R es el número de dígitos del resultado
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        cont2 = 0
        pintaNumeros(digitos, binding.layDigitos)
        creaOperacion()
        pintaNumeros(op1.toString(), binding.layOp1)
        pintaNumeros(op2.toString(), binding.layOp2)
        pintaBtn(mod3)
        pintaNumeros(resultados[mod3].toString(), binding.layResultado)
        binding.layResultado.setOnDragListener(this)
        binding.btnOperacion.setOnClickListener {toggleBtn()}
        if (op1 < op2 && mod3 == 1) { pintaSignoMenos() }
    }

    private fun reinicia() {
        //Anko es maravilloso... usar hilos estándar para modificar vistas es muy difícil
        binding.btnOperacion.isEnabled = false
        doAsync {
            Thread.sleep(2500)
            uiThread {
                binding.btnOperacion.isEnabled = true
                binding.layDigitos.removeAllViews()
                binding.layOp1.removeAllViews()
                binding.layOp2.removeAllViews()
                binding.layResultado.removeAllViews()
                binding.btnOperacion.removeAllViews()
                binding.laySignoMenos.removeAllViews()
                init()
            }
        }
    }

    private fun pintaNumeros(nums: String, ll: LinearLayout) {
        var iv: ImageView
        var id: Int
        var lp: LinearLayout.LayoutParams
        for (i in nums.indices) {
            iv = ImageView(this)
            id = resources.getIdentifier("n${nums[i]}", "drawable", packageName)
            iv.setImageResource(id)
            lp = LinearLayout.LayoutParams(200, 200)
            iv.layoutParams = lp
            if (ll.id == binding.layDigitos.id) {
                iv.tag = i
                iv.setOnTouchListener(this)
            } else if (ll.id == binding.layResultado.id) {
                iv.tag = "${(resultados[mod3].toString())[i]}".toInt()
                iv.visibility = View.INVISIBLE
            }
            ll.addView(iv)
        }
    }

    private fun creaOperacion() {
        op1 = creaOperando()
        op2 = creaOperando()
        creaResultados()
    }

    private fun creaOperando(): Int {
        var op = ""
        val digits = (Math.random()*4).toInt() + 1
        for (i in 0 until digits) {
            op += (Math.random()*9).toInt().toString()
        }
        return op.toInt()
    }

    private fun creaResultados() {
        resultados[0] = op1+op2
        resultados[1] = abs(op1-op2)
        resultados[2] = op1*op2
    }

    private fun toggleBtn() {
        cont2 = 0
        mod3 = cont1 % 3
        cont1++
        pintaBtn(mod3)
        binding.layResultado.removeAllViews()
        binding.laySignoMenos.removeAllViews()
        pintaNumeros(resultados[mod3].toString(), binding.layResultado)
        if (op1 < op2 && mod3 == 1) { pintaSignoMenos() }

        //FUNCIONA!! :)
        //Modifico la direccion del scrollResultado para que vaya de derecha a izquierda
        binding.scrollResultado.postDelayed( {binding.scrollResultado.fullScroll(HorizontalScrollView.FOCUS_RIGHT)},
                3)
    }

    private fun pintaBtn(m: Int) {
        val iv = ImageView(this)
        val lp = LinearLayout.LayoutParams(200, 200)
        iv.layoutParams = lp
        iv.setImageResource(operadores[m])
        binding.btnOperacion.removeAllViews()
        binding.btnOperacion.addView(iv)
    }
    
    private fun pintaSignoMenos() {
        val menos = ImageView(this)
        menos.setImageResource(resources.getIdentifier("signomenos",
                "drawable", packageName))
        val lp = LinearLayout.LayoutParams(200, 200)
        menos.layoutParams = lp
        binding.laySignoMenos.addView(menos)
    }

    //FUNCIONA!! :)
    //onTouch modificado para que maneje vistas DragAndDrop dentro de un ScrollView
    override fun onTouch(v: View?, e: MotionEvent?): Boolean {
        when (e!!.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = e.y
                //Devolver 'true' implica que el focus está en la vista hijita seleccionada (las iv
                //de los numeros), y por tanto se ejecuta este bloque de código en la MainActivity.
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                movedY = e.y
                deltaY = abs(downY - movedY)
                if (deltaY > MIN_DISTANCE_Y) {
                    val shadowBuilder = View.DragShadowBuilder(v)
                    v!!.startDragAndDrop(null, shadowBuilder, v, 0)
                    return true
                }
            }
            else -> return false
        }
        //Devolver 'false' implica que el focus de este evento está en la vista padre (ScrollView),
        //y por tanto se ejecuta el método por defecto de su interfaz en la MainActivity, y no éste.
        return true
    }

    override fun onDrag(v: View?, e: DragEvent): Boolean {
        //Había que retornar 'true' en la propiedad 'ACTION_DRAG_STARTED' para luego
        //poder usar la propiedad 'ACTION_DROP', si no no funcionaba el onDraglistener() en la vista
        //Este detalle me ha llevado resolverlo 1 día entero....
        return when (e.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DROP -> {
                modR = cont2 % binding.layResultado.childCount
                val ivSoltada = e.localState as ImageView
                val indiceResultado = binding.layResultado.childCount - modR - 1
                if (ivSoltada.tag == binding.layResultado.getChildAt(indiceResultado).tag) {
                    binding.layResultado.getChildAt(indiceResultado).visibility = View.VISIBLE
                    cont2++
                    compruebaResultado()
                    true
                } else {
                    false
                }
            }
            else -> false
        }

    }

    private fun compruebaResultado() {
        var bool = true
        binding.layResultado.children.forEach {
            if (!it.isVisible) {
                bool = false
            }
        }
        if (bool) {
            felicita()
            reinicia()
        }
    }

    private fun felicita() {
        Toast.makeText(this, "Enhorabuena champion!! Ahora a por la siguiente...",
                Toast.LENGTH_SHORT).show()
    }
}