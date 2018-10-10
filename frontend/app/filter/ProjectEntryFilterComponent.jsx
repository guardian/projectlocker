import GenericEntryFilterComponent from './GenericEntryFilterComponent.jsx';
import {validateVsid} from "../validators/VsidValidator.jsx";
import PropTypes from 'prop-types';
import axios from 'axios';

class ProjectEntryFilterComponent extends GenericEntryFilterComponent {
    static propTypes = {
        filterDidUpdate: PropTypes.func.isRequired, //this is called when the filter state should be updated. Passed a
        //key-value object of the terms.
        filterTerms: PropTypes.object.isRequired
    };

    componentWillMount(){
        this.setState({distinctOwners: []},()=> {
            axios.get("/api/project/distinctowners")
                .then(result => this.setState({distinctOwners: ["Everyone"].concat(result.data.result.sort())}))
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
                key: "title",
                label: "Title",
                //this is a called for every update. if it returns anything other than NULL it's considered an
                //error and displayed alongside the control
                validator: (input)=>null
            },
            {
                key: "vidispineId",
                label: "PLUTO project id",
                validator: validateVsid
            },
            {
                key: "filename",
                label: "Project file name",
                validator: (input)=>null
            },
            {
                key: "user",
                label: "Owner",
                valuesStateKey: "distinctOwners"
            }
        ];
    }
}

export default ProjectEntryFilterComponent;