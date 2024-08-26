import com.epotheke.erezept.model.AvailablePrescriptionLists
import com.epotheke.erezept.model.SelectedPrescriptionList
import com.epotheke.erezept.model.SelectedPrescriptionListResponse
import com.epotheke.erezept.model.prescriptionJsonFormatter
import kotlinx.serialization.encodeToString

class JsonHelper {
    fun getSelectedPrescriptionListFrom(jsonString: String): SelectedPrescriptionList {
        return prescriptionJsonFormatter.decodeFromString<SelectedPrescriptionList>(jsonString)
    }

    fun availablePrescriptionListsJsonToString(prescriptionLists: AvailablePrescriptionLists): String {
        return prescriptionJsonFormatter.encodeToString(prescriptionLists);
    }

    fun selectedPrescriptionListResponseJsonToString(selection: SelectedPrescriptionListResponse): String {
        return prescriptionJsonFormatter.encodeToString(selection)
    }

}
