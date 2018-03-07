import GenericEntryFilterComponent from './GenericEntryFilterComponent.jsx';
import {validateInt} from "../validators/NumericValidator.jsx";
import PropTypes from 'prop-types';

class FileEntryFilterComponent extends GenericEntryFilterComponent {
    static propTypes = {
        filterDidUpdate: PropTypes.func.isRequired //this is called when the filter state should be updated. Passed a
        //key-value object of the terms.
    };

    constructor(props){
        super(props);

        this.filterSpec = [
            {
                key: "filePath",
                label: "File path",
                //this is a called for every update. if it returns anything other than NULL it's considered an
                //error and displayed alongside the control
                validator: (input)=>null
            },
            {
                key: "storageId",
                label: "Storage ID",
                //validator: (input)=>vsidValidator.test(input) ? null : "This must be in the form of XX-nnnnn"
                validator: validateInt
            },
            {
                key: "user",
                label: "Owner",
                validator: (input)=>null
            }
        ];
    }
}

export default FileEntryFilterComponent;