package com.zhpooer.zio.dojo.domain

import zio.Has

package object todo {
  type TodoRepository = Has[TodoRepository.Service]
  type TodoApplicationService = Has[TodoApplicationService.Service]
}
