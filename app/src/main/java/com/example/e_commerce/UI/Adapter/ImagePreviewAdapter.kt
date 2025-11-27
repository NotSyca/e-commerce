package com.example.e_commerce.UI.Adapter // Paquete donde residen los adaptadores

import android.net.Uri // Import para manejar las URIs de las imágenes
import android.view.LayoutInflater // Import para inflar layouts XML
import android.view.ViewGroup // Import del contenedor padre en una lista
import androidx.recyclerview.widget.RecyclerView // Import de la clase base para adaptadores
import coil.load // Extensión de la librería Coil para cargar imágenes fácilmente
import com.example.e_commerce.databinding.ItemImagePreviewBinding

class ImagePreviewAdapter(private val uris: List<Uri>) : // El constructor recibe la lista de URIs
    RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {

    // El ViewHolder contiene la referencia al layout inflado (a través de ViewBinding).
    inner class ViewHolder(val binding: ItemImagePreviewBinding) : RecyclerView.ViewHolder(binding.root)

    // Se llama cuando el RecyclerView necesita un nuevo ViewHolder.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Infla el layout 'item_image_preview.xml' usando ViewBinding.
        val binding = ItemImagePreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Crea y devuelve la instancia del ViewHolder.
        return ViewHolder(binding)
    }

    // Se llama para vincular los datos de una posición específica con un ViewHolder.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Usa la librería Coil para cargar la imagen desde la URI en el ImageView del item.
        holder.binding.ivPreviewItem.load(uris[position])
    }

    // Devuelve la cantidad total de items en la lista.
    override fun getItemCount(): Int = uris.size
}
