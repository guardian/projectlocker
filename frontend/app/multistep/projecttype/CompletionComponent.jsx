import React from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import CommonCompletionComponent from '../common/CommonCompletionComponent.jsx';

class ProjectTypeCompletionComponent extends CommonCompletionComponent {
    static propTypes = {
        currentEntry: PropTypes.number.isRequired,
        postrunActions: PropTypes.array.isRequired,
        selectedPostruns: PropTypes.array.isRequired,
        originalPostruns: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            inProgress: false,
            newId: null,
            error: null
        };
        this.endpoint = "/api/projecttype"; // override this to the api endpoint that you want to hit
        this.successRedirect = "/type/";    //override this to the page to go to when successfully saved

        this.recordDidSave = this.recordDidSave.bind(this);
    }

    componentWillMount(){
        this.updateAddRemoveDeps();
    }

    /*work out which dependencies need to be added and which removed, and add those to the state*/
    updateAddRemoveDeps(){
        this.setState({
            depsToRemove: this.props.originalPostruns.filter(depId=>!this.props.selectedPostruns.includes(depId)),
            depsToAdd: this.props.selectedPostruns.filter(depId=>!this.props.originalPostruns.includes(depId))
        });
    }

    recordDidSave(){
        const myId = this.props.currentEntry ? this.props.currentEntry : this.state.newId;

        //first add any postruns associations that need adding, then remove postrun associations that need removing.
        return Promise.all(this.state.depsToAdd.map(depId=>axios.put("/api/postrun/" + depId + "/projecttype/" + myId))).then(
            Promise.all(this.state.depsToRemove.map(depId=>axios.delete("/api/postrun/" + depId + "/projecttype/" + myId)))
        );
    }

    requestContent(){
        /* returns an object of keys/values to send to the server for saving */
        let result = {
            name: this.props.projectType.name,
            opensWith: this.props.projectType.opensWith,
            targetVersion: this.props.projectType.version,
            fileExtension: this.props.projectType.fileExtension
        };
        if(this.props.projectType.plutoType) result.plutoType = this.props.projectType.plutoType;
        if(this.props.projectType.plutoSubtype) result.plutoSubtype = this.props.projectType.plutoSubtype;

        return result;
    }

    render() {
        return(<div>
            <h3>Set up project type</h3>
            <p className="information">We will set up a new project type definition with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>
            <SummaryComponent name={this.props.projectType.name}
                              opensWith={this.props.projectType.opensWith}
                              version={this.props.projectType.version}
                              fileExtension={this.props.projectType.fileExtension}
                              plutoType={this.props.projectType.plutoType}
                              plutoSubtype={this.props.projectType.plutoSubtype}
                              postrunActions={this.props.postrunActions}
                              selectedPostruns={this.props.selectedPostruns}
            />
            <p className="information"><b>Note:</b> if you have set a Pluto type and/or subtype identifier, there can only be one instance of each
                type and subtype combination in the system.  If a combination already exists, you will not be able to save this record and
                will see a 409 error.  In this case, you'll need to open a fresh tab, find the existing records and remove the type/subtype
                identifiers from them before retrying the save.</p>
            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default ProjectTypeCompletionComponent;
