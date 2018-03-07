import axios from "axios";
import React from 'react';

class CommonCompletionComponent extends React.Component {
    constructor(props){
        super(props);
        this.endpoint = "/api/unknown"; // override this to the api endpoint that you want to hit
        this.successRedirect = "/unknown/";    //override this to the page to go to when successfully saved

        this.confirmClicked = this.confirmClicked.bind(this);
    }

    /* this method should create a json object from the component's state and should be implemented in your subclass */
    requestContent(){
        throw "Not implemented";
    }

    /*confirmClicked handler to send data to the server*/
    confirmClicked(event){
        this.setState({inProgress: true});
        const endpoint = this.props.currentEntry ? this.endpoint + "/" + this.props.currentEntry : this.endpoint;

        axios.request({method: "PUT", url: endpoint,data: this.requestContent()}).then(
            (response)=>{
                this.setState({inProgress: false});
                window.location.assign(this.successRedirect);
            }
        ).catch(
            (error)=>{
                this.setState({inProgress: false, error: error});
                console.error(error)
            }
        )
    }
}

export default CommonCompletionComponent;