import React from 'react';
import axios from 'axios';
import ErrorViewComponent from '../multistep/common/ErrorViewComponent.jsx';
import PropTypes from 'prop-types';

class GeneralDeleteComponent extends React.Component {
    static propTypes = {
        match: PropTypes.object.isRequired,
        history: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);

        this.endpoint = "/api/unknown";
        this.state = {
            loading: true,
            selectedItem: null,
            error: null,
            completedOperation: false,
            warning: null
        };

        this.confirmClicked = this.confirmClicked.bind(this);
        this.cancelClicked = this.cancelClicked.bind(this);
    }

    /* you can over-ride this in a subclass to do something once the main data has downloaded*/
    postDownload(){

    }

    /*download information about the thing we want to delete*/
    componentWillMount() {
        axios.get(this.endpoint + "/" + this.props.match.params.itemid).then((response)=>{
            this.setState({loading: false, selectedItem: response.data.result},()=>this.postDownload())
        }).catch((error)=>{
            console.error(error);
            this.setState({loading: false, error: error});
        });
    }

    /*this should be over-ridden by subclasses to render a summary of the object to be deleted*/
    getSummary() {
        return <p>No summary</p>;
    }

    /*run when the Delete button is clicked*/
    confirmClicked(event) {
        axios.delete(this.endpoint + "/" + this.props.match.params.itemid).then((response)=>{
            if(response.data.status==="warning"){
                this.setState({warning: response.data.detail, completedOperation: true});
            } else {
                this.props.history.goBack();
            }
        }).catch(error=>{
            this.setState({error: error, completedOperation: true});
        })
    }

    /*run when the Cancel button is clicked*/
    cancelClicked(event) {
        this.props.history.goBack();
    }

    informationPara() {
        if(this.state.warning)
            return <p className="warning">{this.state.warning}</p>;
        else if(!this.state.error)
            return <p className="information">The following {this.itemClass} will be PERMANENTLY deleted, if you click the
                Delete button below.  Do you want to continue?</p>;
        else
            return <p className="information"/>
    }

    controls() {
        if(this.state.warning || this.state.error){
            return <span style={{float: "right"}}><button id="cancelButton" onClick={this.cancelClicked}>Close</button></span>
        } else {
            return <span>
            <span style={{float: "right"}}><button id="deleteButton" onClick={this.confirmClicked}>Delete</button></span>
            <span style={{float: "left"}}><button id="cancelButton" onClick={this.cancelClicked}>Cancel</button></span>
        </span>
        }
    }

    render(){
        if(this.state.loading) return <p className="information">Loading...</p>;

        return <div className="filter-list-block">
            <h3>Delete {this.itemClass}</h3>
            {this.informationPara()}
            {this.getSummary()}
            <ErrorViewComponent error={this.state.error}/>
            {this.controls()}
        </div>;
    }
}

export default GeneralDeleteComponent;