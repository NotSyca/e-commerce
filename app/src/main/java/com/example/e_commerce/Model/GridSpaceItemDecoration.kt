package com.example.e_commerce.Model // Puedes ajustarlo a tu carpeta de utilidades

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager

class GridSpaceItemDecoration(
    private val spacing: Int, // Usaremos 'spacing' como el único parámetro relevante para el espaciado.
    spacing1: Int,
    bool: Boolean
    // Los otros parámetros (spacing: Int, bool: Boolean) no son necesarios en el constructor con tu lógica actual.
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // Posición del ítem

        // Obtener LayoutManager y salir si no es GridLayoutManager
        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return

        val spanCount = layoutManager.spanCount // Número de columnas
        val column = position % spanCount // Columna actual (0, 1, 2, etc.)

        // La variable 'spacing' se refiere al parámetro del constructor (el tamaño del espacio en píxeles).

        // Espacio horizontal:
        // Calcula el espaciado para que sea uniforme entre los ítems.
        // Fórmula original (usando 'spacing' como el tamaño del espacio):
        outRect.left = spacing - column * spacing / spanCount
        outRect.right = (column + 1) * spacing / spanCount

        // Espacio vertical:
        if (position < spanCount) { // Fila superior
            outRect.top = spacing // Espacio arriba de la primera fila
        }
        outRect.bottom = spacing // Espacio debajo de cada ítem (para separar filas)
    }
}