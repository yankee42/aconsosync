package com.github.yankee42.aconsosync.gui

import javax.swing.SwingWorker

fun swingWorker(worker: (() -> Unit)) = object : SwingWorker<Unit, Unit>() {
    override fun doInBackground(): Unit = worker()
    override fun done() = get() // call get so that exceptions propagate
}.execute()
