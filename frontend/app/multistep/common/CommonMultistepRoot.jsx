import React from 'react';

class CommonMultistepRoot extends React.Component {
    componentWillMount(){
        if(this.props.match && this.props.match.params && this.props.match.params.itemid && this.props.match.params.itemid!=="new"){
            console.log("commonMultistepRoot: got item id", this.props.match.params.itemid);
            this.setState({currentEntry: this.props.match.params.itemid}, ()=>{
                this.loadDependencies().then(()=>this.loadEntity(parseInt(this.props.match.params.itemid)));
            })
        } else {
            console.log("commonMultistepRoot: got no item id");
            this.loadDependencies();
        }
    }

    /* this method should be overridden in a subclass to load in the requested entity*/
    loadEntity(itemId) {

    }
    /* this method can be overridden in a subclass to load in any extra data required; should return a promise in order to be chained*/
    loadDependencies(){

    }
}

export default CommonMultistepRoot;