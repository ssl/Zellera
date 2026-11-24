package zel.protocol.generated

data object GetSoundMachinePlayListIn

object GetSoundMachinePlayListInCodec : IncomingCodec<GetSoundMachinePlayListIn> {
    override val name = "GetSoundMachinePlayListIn"

    override fun read(r: PacketReader): GetSoundMachinePlayListIn = GetSoundMachinePlayListIn

    override fun write(w: PacketWriter, m: GetSoundMachinePlayListIn) = writeGetSoundMachinePlayListIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetSoundMachinePlayListIn(w: PacketWriter, m: GetSoundMachinePlayListIn) {
    }
}
