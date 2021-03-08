package gr.papadogiannis.stefanos.messages

final case class ReplyReducerList(requestId: Long, ids: Set[String])
