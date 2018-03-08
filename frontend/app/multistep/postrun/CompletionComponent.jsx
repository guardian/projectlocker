import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import CommonCompletionComponent from '../common/CommonCompletionComponent.jsx';

class CompletionComponent extends CommonCompletionComponent {
    static propTypes = {
        postrunMetadata: PropTypes.object.isRequired,
        postrunSource: PropTypes.string.isRequired,
        currentEntry: PropTypes.number.isRequired,
        actionList: PropTypes.array.isRequired,
        selectedDependencies: PropTypes.array.isRequired,
        originalDependencies: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);
        this.state = {
            loading: false,
            loadingError: null,
            depsToRemove: [],
            depsToAdd: []
        };
        this.endpoint = "/api/postrun";
        this.successRedirect = "/postrun/";

        this.recordDidSave = this.recordDidSave.bind(this);
    }

    requestContent(){
        return this.props.postrunMetadata;
    }

    componentWillMount(){
        this.updateAddRemoveDeps();
    }

    /*work out which dependencies need to be added and which removed, and add those to the state*/
    updateAddRemoveDeps(){
        this.setState({
            depsToRemove: this.props.originalDependencies.filter(depId=>!this.props.selectedDependencies.includes(depId)),
            depsToAdd: this.props.selectedDependencies.filter(depId=>!this.props.originalDependencies.includes(depId))
        });
    }

    /*remove any dependencies that have been deselected*/
    doRemoveDeps(){
        Promise.all(this.state.depsToRemove.map(depId=>axios.delete("/api/postrun/" + this.props.currentEntry + "/depends/" + depId)));
    }

    /* add any dependencies that have been selected */
    doAddDeps(){
        Promise.all(this.state.depsToAdd.map(depId=>axios.put("/api/postrun/" + this.props.currentEntry + "/depends/" + depId)));
    }

    recordDidSave(){
        return Promise.all(this.state.depsToRemove.map(depId=>axios.delete("/api/postrun/" + this.props.currentEntry + "/depends/" + depId)))
            .then(Promise.all(this.state.depsToAdd.map(depId=>axios.put("/api/postrun/" + this.props.currentEntry + "/depends/" + depId))))
    }

    render(){
        return <div>
            <h3>Edit postrun action metadata</h3>
            <p className="information">The postrun action information will be edited as below. Click Confirm to continue or Back to change.</p>
            <SummaryComponent title={this.props.postrunMetadata.title}
                              description={this.props.postrunMetadata.description}
                              actionList={this.props.actionList}
                              selectedActions={this.props.selectedDependencies}
            />
            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>
    }
}

export default CompletionComponent;
