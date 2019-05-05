package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_13;

import java.util.ArrayList;
import java.util.List;

import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.ClientBoundPacket;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleChunkLight;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.serializer.ItemStackSerializer;
import protocolsupport.protocol.serializer.PositionSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.block.FlatteningBlockData;
import protocolsupport.protocol.typeremapper.block.FlatteningBlockData.FlatteningBlockDataTable;
import protocolsupport.protocol.typeremapper.block.LegacyBlockData;
import protocolsupport.protocol.typeremapper.chunknew.ChunkWriterVariesWithLight;
import protocolsupport.protocol.typeremapper.utils.RemappingTable.ArrayBasedIdRemappingTable;
import protocolsupport.protocol.utils.chunk.ChunkConstants;
import protocolsupport.protocol.utils.types.TileEntity;
import protocolsupport.utils.Utils;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableEmptyList;
import protocolsupport.utils.recyclable.RecyclableSingletonList;

public class ChunkLight extends MiddleChunkLight {

	protected final ArrayBasedIdRemappingTable blockDataRemappingTable = LegacyBlockData.REGISTRY.getTable(version);
	protected final FlatteningBlockDataTable flatteningBlockDataTable = FlatteningBlockData.REGISTRY.getTable(version);

	public ChunkLight(ConnectionImpl connection) {
		super(connection);
	}

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		if (preChunk) {
			return RecyclableEmptyList.get();
		} else {
			int blockMask = ((setSkyLightMask | setBlockLightMask | emptySkyLightMask | emptyBlockLightMask) >> 1) & 0xFFFF;
			boolean hasSkyLight = cache.getAttributesCache().hasSkyLightInCurrentDimension();
			List<TileEntity> resendTiles = new ArrayList<>();

			ClientBoundPacketData serializer = ClientBoundPacketData.create(ClientBoundPacket.PLAY_CHUNK_SINGLE_ID);
			PositionSerializer.writeIntChunkCoord(serializer, coord);
			serializer.writeBoolean(false); //full
			VarNumberSerializer.writeVarInt(serializer, blockMask);
			ArraySerializer.writeVarIntByteArray(serializer, to -> {
				for (int sectionNumber = 0; sectionNumber < ChunkConstants.SECTION_COUNT_BLOCKS; sectionNumber++) {
					if (Utils.isBitSet(blockMask, sectionNumber)) {
						ChunkWriterVariesWithLight.writeSectionDataFlattening(
							to,
							14, blockDataRemappingTable, flatteningBlockDataTable,
							cachedChunk, hasSkyLight, sectionNumber
						);
						resendTiles.addAll(cachedChunk.getTiles(sectionNumber).values());
					}
				}
			});

			ArraySerializer.writeVarIntTArray(
				serializer,
				resendTiles,
				(to, tile) -> ItemStackSerializer.writeTag(to, version, tile.getNBT())
			);

			return RecyclableSingletonList.create(serializer);
		}
	}

}