import React from 'react';
import axios from 'axios';

class GeneralDeleteComponent extends React.Component {
    constructor(props){
        super(props);

        this.endpoint = "/api/unknown";
        this.state = {
            selectedItem: null,
            error: null
        };

        this.confirmClicked = this.confirmClicked.bind(this);
        this.cancelClicked = this.cancelClicked.bind(this);
    }

    /*download information about the thing we want to delete*/
    componentWillMount() {
        axios.get(this.endpoint + "/" + this.props.match.params.itemid).then((response)=>{
            this.setState({selectedItem: response.data.result})
        }).catch((error)=>console.error(error));
    }

    /*this should be over-ridden by subclasses to render a summary of the object to be deleted*/
    getSummary() {
        return <p>No summary</p>;
    }

    /*run when the Delete button is clicked*/
    confirmClicked(event) {
        axios.delete(this.endpoint + "/" + this.props.match.params.itemid).then((response)=>{
            this.props.history.goBack();
        }).catch(error=>{
            this.setState({error: error});
        })
    }

    /*run when the Cancel button is clicked*/
    cancelClicked(event) {
        this.context.router.history.goBack();
    }

    render(){
        if(!this.state.selectedItem) return <p className="information">Loading...</p>;
        //if(this.state.error) return <p className="error">{this.state.error.message}</p>;

        return <div>
            <h3>Delete {this.itemClass}</h3>
            <p className="information">The following {this.itemClass} will be PERMANENTLY deleted, if you click the
            Delete button below.  Do you want to continue?</p>
            {this.getSummary()}
            <span style={{float: "right"}}><button id="deleteButton" onClick={this.confirmClicked}>Delete</button></span>
            <span style={{float: "left"}}><button id="cancelButton" onClick={this.cancelClicked}>Cancel</button></span>
        </div>;
    }
}

export default GeneralDeleteComponent;