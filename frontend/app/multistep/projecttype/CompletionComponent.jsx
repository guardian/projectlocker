import React from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import CommonCompletionComponent from '../common/CommonCompletionComponent.jsx';

class ProjectTypeCompletionComponent extends CommonCompletionComponent {
    static propTypes = {
        currentEntry: PropTypes.object.isRequired,
        postrunActions: PropTypes.array.isRequired,
        selectedPostruns: PropTypes.array.isRequired,
        originalPostruns: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            inProgress: false,
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


    savePostruns(projecttypeid) {
        const promiseList = this.props.selectedPostruns.map(postrunId => axios.put("/api/postrun/" + postrunId + "/projecttype/" + projecttypeid))
        return Promise.all(promiseList)
    }

    recordDidSave(){
        //first add any postruns associations that need adding, then remove postrun associations that need removing.
        return Promise.all(this.state.depsToAdd.map(depId=>axios.put("/api/postrun/" + depId + "/projecttype/" + this.props.currentEntry))).then(
            Promise.all(this.state.depsToRemove.map(depId=>axios.delete("/api/postrun/" + depId + "/projecttype/" + this.props.currentEntry)))
        );
    }

    requestContent(){
        /* returns an object of keys/values to send to the server for saving */
        return {
            name: this.props.projectType.name,
            opensWith: this.props.projectType.opensWith,
            targetVersion: this.props.projectType.version,
            fileExtension: this.props.projectType.fileExtension
        }
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
                              postrunActions={this.props.postrunActions}
                              selectedPostruns={this.props.selectedPostruns}
            />
            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default ProjectTypeCompletionComponent;
