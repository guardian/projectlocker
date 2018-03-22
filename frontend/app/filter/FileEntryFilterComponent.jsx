import GenericEntryFilterComponent from './GenericEntryFilterComponent.jsx';
import {validateInt} from "../validators/NumericValidator.jsx";
import PropTypes from 'prop-types';
import axios from 'axios';

class FileEntryFilterComponent extends GenericEntryFilterComponent {
    static propTypes = {
        filterDidUpdate: PropTypes.func.isRequired, //this is called when the filter state should be updated. Passed a
        //key-value object of the terms.
        isAdmin: PropTypes.bool,
        initialFilters: PropTypes.object
    };

    componentWillMount(){
        this.setState({distinctOwners: []},()=> {
            axios.get("/api/file/distinctowners")
                .then(result => this.setState({distinctOwners: result.data.result}))
                .catch(error => {
                    console.error(error);
                    this.setState({error: error});
                });
        });
    }

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
                validator: validateInt,
                converter: value=>parseInt(value)
            },
            {
                key: "user",
                label: "Owner",
                valuesStateKey: "distinctOwners",
                disabledIfNotAdmin: true,
                validator: (input)=>null
            }
        ];
    }
}

export default FileEntryFilterComponent;