package com.zhpooer.zio.dojo.domain.todo

import org.http4s.Uri

case class Todo(
  id: Int,
  title: String,
  order:Int,
  completed: Boolean,
  url: Uri
)

