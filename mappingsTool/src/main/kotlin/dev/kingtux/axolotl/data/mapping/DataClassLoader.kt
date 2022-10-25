package dev.kingtux.axolotl.data.mapping

import java.net.URL
import java.net.URLClassLoader

class DataClassLoader(url: Array<URL>) : URLClassLoader("DataBuilder", url, DataClassLoader::class.java.classLoader) {
    override fun loadClass(name: String?): Class<*> {
        return super.loadClass(name)
    }

    public override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        return super.loadClass(name, resolve)
    }

    public override fun addURL(url: URL?) {
        super.addURL(url)
    }
}