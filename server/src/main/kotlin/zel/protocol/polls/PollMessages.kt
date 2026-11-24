package zel.protocol.polls

import zel.game.polls.Poll
import zel.game.polls.Question
import zel.protocol.generated.PollContentsOut

fun pollContents(poll: Poll): PollContentsOut =
    PollContentsOut(
        poll.id,
        poll.title,
        poll.thanksMessage,
        poll.questions.map(::topQuestion),
        true,
    )

private fun choices(question: Question) =
    question.choices.map { PollContentsOut.List1Item.List1Item(it.value, it.text, it.category) }

private fun topQuestion(question: Question): PollContentsOut.List1Item =
    PollContentsOut.List1Item(
        question.id,
        question.position,
        question.kind,
        question.text,
        question.category,
        question.kind,
        choices(question),
        question.followUps.map(::subQuestion),
    )

private fun subQuestion(question: Question): PollContentsOut.List1Item.List2Item =
    PollContentsOut.List1Item.List2Item(
        question.id,
        question.position,
        question.kind,
        question.text,
        question.category,
        question.kind,
        choices(question).map { PollContentsOut.List1Item.List2Item.List1Item(it.string1, it.string2, it.int1) },
    )
